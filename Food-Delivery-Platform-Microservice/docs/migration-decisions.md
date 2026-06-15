# Migration Decisions

This document explains the architectural choices made when decomposing a hypothetical food delivery monolith into microservices.

---

## Service Boundaries

### Why four business services?

The decomposition follows the **strangler fig pattern**: identify bounded contexts by asking which domain objects change together and which teams would own them independently.

| Service | Bounded Context | Change driver |
|---|---|---|
| Customer Service | Identity and authentication | Auth mechanism, profile schema |
| Restaurant Service | Catalogue and menus | Menu structure, owner roles |
| Order Service | Order lifecycle | Business rules, pricing logic |
| Delivery Service | Logistics | Driver assignment, geo-routing |

A single change to delivery assignment logic (e.g. introducing geo-based driver matching) should not require redeploying auth or menu code. Separate deployability is the primary motivation, not team size.

### Why not split further?

Payment processing is a natural fifth service but was excluded from this scope. The `estimatedDeliveryTime` field on `Order` and rating on `Restaurant` are placeholders for future services (scheduling, review). The rule applied: **each service must justify its own deployment boundary** — splitting artificially produces coordination overhead without isolation benefit.

---

## Database-per-Service

Each service owns its schema exclusively. No service reads another service's database directly.

**Why:** Shared databases couple services at the schema level. A column rename in `customers` would break Order Service's queries without any code change in Order Service. The database is part of the service's private API.

**Trade-off:** Cross-service queries (e.g. "all orders with customer name") require either a Feign call or an event-driven projection. This is intentional — it forces the consumer to declare what data it actually needs from the producer.

**Dev convenience:** Customer, Restaurant, and Delivery use H2 in-memory with `create-drop` so they start without external infrastructure. Order Service uses PostgreSQL even locally because order data is the system of record and losing it on restart during development is disruptive.

---

## Synchronous vs Asynchronous Communication

### Why Feign (synchronous) for Order → Customer and Order → Restaurant?

Order placement requires validating both the customer and the menu items **before** the order can be confirmed. If Customer Service is down, the order must be rejected — not silently accepted and processed later. Feign with circuit breakers gives us:

- Real-time validation at placement time
- A fast failure path (circuit breaker → 503) rather than silent data corruption
- A clear causal chain in the HTTP response

### Why RabbitMQ (asynchronous) for Order → Delivery?

Delivery assignment does not need to complete synchronously within the `POST /api/orders` response. The customer receives order confirmation the moment the order is persisted; delivery dispatch is a follow-on action.

Using a message queue here means:
- Delivery Service can be down during order placement without failing the order
- Failed delivery creation retries via the dead-letter queue rather than requiring a client retry
- The order-to-delivery coupling is one-way: Order Service does not depend on Delivery Service being discoverable

The asymmetry is deliberate: **validation is synchronous; side effects are asynchronous**.

### Why a dead-letter exchange?

Without a DLQ, a message that fails processing (e.g. deserialization error, transient DB failure) is either silently dropped or causes an infinite retry loop that blocks the queue. The `order.dlx` / `order.dlq` pattern holds poison messages for manual inspection while the main queue continues processing healthy messages.

Both Order Service and Delivery Service declare the same `order.placed.queue` with identical DLQ arguments to avoid `PRECONDITION_FAILED` from RabbitMQ when either service starts first.

---

## Centralized JWT Validation

JWT validation lives in the API Gateway (`JwtAuthenticationFilter`). Services do not re-validate tokens.

**Why:** Duplicating validation logic across four services means four places to update the secret, four places to get the algorithm wrong, and four services that need the JWT library on their classpath. The gateway is the trust boundary.

**How it works:** After validation, the gateway injects `X-Username`, `X-User-Role`, and `X-Customer-Id` headers. Services read these headers via `GatewayHeaderFilter` and build a `SecurityContext` without touching the token. This makes each service's security configuration simple and testable in isolation.

**Production note:** The service ports (8081–8084) must not be exposed outside the cluster. Only the gateway port (8080) should be accessible. In Docker, all services are on the `food-delivery-net` bridge network and the only published port is 8080.

---

## Rate Limiting on POST /api/orders

The rate limiter targets `POST /api/orders` only, not all traffic.

**Why this endpoint:** Order placement is the most expensive operation in the system — it triggers two Feign calls and a RabbitMQ publish in addition to the database write. A burst of order requests amplifies load on Customer Service, Restaurant Service, and the message broker simultaneously.

**Why `timeoutDuration = ZERO`:** The gateway uses Spring WebFlux (reactive). Blocking a thread while waiting for a rate limit permit would defeat the purpose of the reactive model. `ZERO` timeout makes `acquirePermission()` a non-blocking try-acquire: it returns `false` immediately if no permit is available, and the filter responds with 429 without touching the event loop.

---

## Circuit Breakers on Feign Clients

Circuit breakers protect Order Service and Restaurant Service from cascading failures when their upstream dependencies are slow or unavailable.

**Why `group.enabled: true`:** Without grouping, Resilience4j creates a separate circuit breaker instance per Feign method — e.g. `CustomerClient#getById(Long)`. This means a failing `getById` call could trip open while `promoteToRestaurantOwner` still sends requests into a degraded service. Grouping all methods of a client under one instance (`customer-service`) gives a more accurate picture of the downstream service's health.

**Why `FallbackFactory` over `@FeignClient(fallback = ...)`:** `FallbackFactory` receives the actual exception that caused the failure. This allows the fallback to log the root cause and rethrow a domain-specific `ServiceUnavailableException` that the `GlobalExceptionHandler` maps to HTTP 503 — giving the caller a meaningful error instead of a generic Feign stack trace.

---

## Package Naming

Customer and Order services use `com.fooddelivery.*`; Restaurant and Delivery use `com.david.*`.

This is a historical inconsistency from parallel development streams, not a design decision. It is intentionally preserved to reflect real-world migration scenarios where services are not uniformly branded. Feign client `name` attributes reference logical service names registered in Eureka (`customer-service`, `restaurant-service`) — not package names — so the inconsistency has no functional impact.
