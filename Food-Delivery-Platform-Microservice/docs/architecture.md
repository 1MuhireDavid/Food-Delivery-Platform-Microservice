# Architecture

## Component Diagram

```
┌───────────────────────────────────────────────────────────┐
│  HTTP Client                                               │
└──────────────────────────┬────────────────────────────────┘
                           │
                           ▼
┌───────────────────────────────────────────────────────────┐
│  API Gateway  :8080                                        │
│                                                            │
│  JwtAuthenticationFilter  (GlobalFilter, order = -1)       │
│    validates HS256 signature · rejects 401 on failure      │
│    injects: X-Username · X-User-Role · X-Customer-Id       │
│                                                            │
│  OrderRateLimiterFilter   (GlobalFilter, order = 0)        │
│    POST /api/orders only · 20 permits/second               │
│    non-blocking (timeoutDuration = ZERO) · 429 on breach   │
└──────┬──────────────────────────────────────────┬─────────┘
       │  Eureka load-balanced routing             │
       ▼                                           ▼
┌──────────────────┐  ┌────────────────┐  ┌───────────────┐  ┌──────────────────┐
│ Customer  :8081  │  │ Restaurant :8082│  │  Order  :8083 │  │ Delivery  :8084  │
│                  │  │                │  │               │  │                  │
│ Auth, Profiles   │  │ Restaurants    │  │ Orders        │  │ Deliveries       │
│ H2 / pg customer │  │ Menus          │  │ Events        │  │ Driver tracking  │
│                  │  │ H2 / pg rest.  │  │ PostgreSQL    │  │ H2 / pg delivery │
└──────────────────┘  └────────────────┘  └───────┬───────┘  └────────┬─────────┘
         ▲                    ▲                    │ Feign             │
         │                    │                    ▼                   │
         └────────────────────┴──────── (CustomerClient)               │
         ▲                                         │ Feign             │
         │                            (RestaurantClient)               │
         └────────────────────────────────────────┘                   │
                                                                        │
         ┌──────────────────────────────────────────────────────────────┘
         │
         ▼
┌───────────────────────────────────────────────────────────┐
│  RabbitMQ  :5672                                           │
│                                                            │
│  order.exchange  (topic, durable)                          │
│    order.placed     → order.placed.queue     (Delivery)    │
│    order.cancelled  → order.cancelled.queue  (Delivery)    │
│                                                            │
│  order.dlx  (direct, dead-letter)                          │
│    order.placed.dlq                                        │
│    order.cancelled.dlq                                     │
│                                                            │
│  delivery.exchange  (topic, durable)                       │
│    delivery.status  → delivery.status.queue                │
└───────────────────────────────────────────────────────────┘
```

## Request Flow — Placing an Order

```
1.  Client        POST /api/orders  Authorization: Bearer <jwt>

2.  Gateway       JwtAuthenticationFilter validates the JWT signature.
                  Extracts claims: username=alice, role=CUSTOMER, customerId=42.
                  Injects headers: X-Username: alice
                                   X-User-Role: CUSTOMER
                                   X-Customer-Id: 42

3.  Gateway       OrderRateLimiterFilter checks 20 req/s permit.
                  Returns 429 + Retry-After: 1 if the bucket is empty.

4.  Gateway       Eureka resolves ORDER-SERVICE → :8083.
                  Forwards request with injected headers.

5.  Order Svc     GatewayHeaderFilter reads injected headers, builds SecurityContext.

6.  Order Svc     Feign → GET /api/customers/internal/42  (Customer Service)
                  Feign → GET /api/restaurants/internal/5  (Restaurant Service)
                  Feign → GET /api/restaurants/internal/menu-item/{id}  (per item)

7.  Order Svc     Persists Order to PostgreSQL.

8.  Order Svc     Publishes OrderPlacedEvent to order.exchange / order.placed.

9.  Delivery Svc  @RabbitListener on order.placed.queue creates Delivery record.
                  Returns immediately; the HTTP response to the client is not blocked.
```

## Synchronous Communication (OpenFeign)

| Caller | Target | Endpoint | Trigger |
|---|---|---|---|
| Order Service | Customer Service | `GET /api/customers/internal/{id}` | Every order placement |
| Order Service | Restaurant Service | `GET /api/restaurants/internal/{id}` | Every order placement |
| Order Service | Restaurant Service | `GET /api/restaurants/internal/menu-item/{itemId}` | Per line item |
| Restaurant Service | Customer Service | `PATCH /api/customers/internal/{username}/promote-to-owner` | Restaurant creation |

All Feign clients use Resilience4j circuit breakers with `group.enabled: true` (one CB instance per client, named after the service). When the circuit is OPEN, a `FallbackFactory` returns a `ServiceUnavailableException` → HTTP 503.

## Asynchronous Communication (RabbitMQ)

| Exchange | Routing Key | Producer | Consumer | On Failure |
|---|---|---|---|---|
| `order.exchange` | `order.placed` | Order Service | Delivery Service | → `order.placed.dlq` |
| `order.exchange` | `order.cancelled` | Order Service | Delivery Service | → `order.cancelled.dlq` |
| `delivery.exchange` | `delivery.status` | Delivery Service | (future consumers) | — |

`DeliveryStatusUpdatedEvent` is published when a delivery transitions to `PICKED_UP` or `DELIVERED`.

## Security Model

JWT validation lives **only** in the API Gateway. Services downstream trust the three injected headers and do not re-validate tokens. This means:

- A direct call to a service port (bypassing the gateway) will be accepted without auth — the service ports must not be publicly accessible in production.
- The JWT secret must be identical in all five `application.yml` files (`app.jwt.secret`).

**Public paths** — gateway lets these through without a JWT:

```
POST /api/auth/register
POST /api/auth/login
GET  /api/restaurants/search/**
GET  /api/restaurants/*/menu
GET  /actuator/**
```

## Circuit Breaker State Machine

```
                  failure rate ≥ 50%
                  (over 10-call window)
  CLOSED ─────────────────────────────────► OPEN
    ▲                                         │
    │                                         │  wait 5 s
    │                                         ▼
    └──────────────────────────────────── HALF-OPEN
          3 probe calls succeed            (3 probes)
```

`minimumNumberOfCalls: 5` — the window must have at least 5 calls before the rate is evaluated.

## Database Layout

| Service | Local dev | Docker (`SPRING_PROFILES_ACTIVE=docker`) |
|---|---|---|
| Customer | H2 in-memory (`create-drop`) | PostgreSQL `customer_db` |
| Restaurant | H2 in-memory (`create-drop`) | PostgreSQL `restaurant_db` |
| Order | PostgreSQL `order_db` | PostgreSQL `order_db` |
| Delivery | H2 in-memory (`create-drop`) | PostgreSQL `delivery_db` |

No cross-service JPA joins exist. Foreign key references between services (e.g. `orders.customer_id`) are stored as plain `Long` columns — ownership stays within the producing service.
