# Testing Guide

## Prerequisites

All services must be running before executing the collection:

```bash
# Docker (recommended — starts everything in the correct order)
docker compose up --build

# Allow ~60 seconds for JVMs to start, then verify
curl http://localhost:8761   # Eureka dashboard
curl http://localhost:8080/actuator/health  # Gateway health
```

For local startup order see the [README](../README.md#local-development-no-docker).

---

## Importing into Postman

1. Open Postman → **Import**
2. Drag both files from `postman/` into the Import window:
   - `Food-Delivery-Platform.postman_collection.json`
   - `Food-Delivery-Platform.postman_environment.json`
3. Select the **Food Delivery Platform — Local** environment from the environment picker (top-right dropdown)

---

## Running the Full Collection

1. Click the collection name → **Run collection**
2. Ensure all folders are checked
3. Set **Delay** to **1000 ms** — this is required so RabbitMQ has time to deliver the `OrderPlacedEvent` before the `[EVENT] Get Delivery by Order` request runs
4. Click **Run Food Delivery Platform — E2E**

The collection is designed to run end-to-end in folder order:

```
01 - Setup          → creates two user accounts
02 - Restaurant Catalog → creates restaurant + menu items, exercises public endpoints
03 - Full Order Flow    → full journey: place order → event → delivery lifecycle
04 - Additional Endpoints → cancel flow, history queries
05 - Fault Tolerance    → requires manual steps (read below)
```

Each test script saves dynamic values (tokens, IDs) to collection variables so subsequent requests use the correct data automatically. There is no manual variable setup required.

---

## Scenario: Full Order Flow (Folder 03)

This folder demonstrates the complete customer journey and is the most important E2E test.

| Step | Request | What it proves |
|---|---|---|
| 1 | Place Order | Order is persisted; `PLACED` status |
| 2 | Verify Order Placed | Data integrity after write |
| 3 | **[EVENT] Get Delivery by Order** | **Delivery was auto-created by Delivery Service consuming the RabbitMQ event — no REST call triggered it** |
| 4 | Update Delivery → ASSIGNED | Normal status progression |
| 5 | Update Delivery → PICKED_UP | Publishes `DeliveryStatusUpdatedEvent` to `delivery.exchange` |
| 6 | Update Delivery → DELIVERED | Final state; `deliveredAt` timestamp set |

### Verifying RabbitMQ event propagation

While folder 03 runs, open the RabbitMQ Management UI at **http://localhost:15672** (credentials: `guest` / `guest`):

1. Go to **Queues**
2. Watch `order.placed.queue`: message count spikes to 1 immediately after "Place Order", then drops to 0 as Delivery Service consumes it
3. Check `delivery.status.queue` after the PICKED_UP and DELIVERED transitions

If `[EVENT] Get Delivery by Order` returns **404**, the event was not yet processed. Increase the Collection Runner delay to 2000 ms and retry.

---

## Scenario: Fault Tolerance (Folder 05)

Each request in this folder requires manual service disruption. **Read the request description before sending.**

### Circuit Breaker — Customer Service Down

```bash
# 1. Stop the service
docker stop food-delivery-customer-service

# 2. Send "[CB] Place Order — Customer Service Down" several times
#    - Calls 1–5: HTTP 503, CustomerClient fallback factory responds
#    - Calls 6+:  HTTP 503, "circuit breaker open: customer-service" (CB is now OPEN)

# 3. Observe circuit breaker state directly
curl http://localhost:8083/actuator/health | jq '.components.circuitBreakers'

# 4. Restore the service
docker start food-delivery-customer-service

# 5. After 5 seconds (waitDurationInOpenState), CB transitions to HALF_OPEN
#    Three successful probe calls close it back to CLOSED
```

Expected 503 response body when CB is open:
```json
{
  "message": "Service temporarily unavailable (circuit breaker open): customer-service"
}
```

### Circuit Breaker — Service Isolation

While Customer Service is down, run `[CB] Service Isolation — Public Search Still Works`. Expected: **HTTP 200**.

This confirms that Restaurant Service's public catalog endpoints do not depend on Customer Service — only the Feign calls (restaurant creation, order placement) are affected.

### Rate Limiter — POST /api/orders

The gateway allows 20 order-placement requests per second per gateway instance.

```
Collection Runner settings for rate limit test:
  - Select only: [429] Rate Limit — POST /api/orders
  - Iterations: 25
  - Delay: 0 ms
```

Expected: The first 20 iterations within the same second return `201` (or `503` from CB if an upstream is degraded). Iterations 21+ return:

```
HTTP 429 Too Many Requests
Retry-After: 1
```

Wait 1 second for the permit bucket to refill.

---

## Inspecting Circuit Breaker State

Each service exposes Actuator health with circuit breaker details. Call these **directly** (not through the gateway):

```bash
curl http://localhost:8081/actuator/health | jq '.components'  # Customer Service
curl http://localhost:8082/actuator/health | jq '.components'  # Restaurant Service
curl http://localhost:8083/actuator/health | jq '.components'  # Order Service
curl http://localhost:8084/actuator/health | jq '.components'  # Delivery Service
```

Circuit breaker state transitions:
```
CLOSED → OPEN (after 50% failure rate over 10-call window, min 5 calls)
OPEN   → HALF_OPEN (after 5 s)
HALF_OPEN → CLOSED (after 3 successful probes)
HALF_OPEN → OPEN   (if any probe fails)
```

---

## Dead Letter Queue Inspection

Failed messages land in the DLQ. To inspect them in RabbitMQ Management:

1. Open http://localhost:15672 → **Queues**
2. Look for `order.placed.dlq` and `order.cancelled.dlq`
3. Click a queue → **Get messages** to view the raw payload

To force a DLQ message: shut down Delivery Service, place an order, wait for RabbitMQ to exhaust retries, restart Delivery Service.

---

## Running with Newman (CLI)

Newman is the command-line Postman runner, useful for CI:

```bash
npm install -g newman

newman run postman/Food-Delivery-Platform.postman_collection.json \
  --environment postman/Food-Delivery-Platform.postman_environment.json \
  --delay-request 1000 \
  --reporters cli,json \
  --reporter-json-export results.json
```

Note: Folder 05 (Fault Tolerance) requires manual service disruption and should be excluded from automated CI runs:

```bash
newman run postman/Food-Delivery-Platform.postman_collection.json \
  --environment postman/Food-Delivery-Platform.postman_environment.json \
  --delay-request 1000 \
  --folder "01 - Setup" \
  --folder "02 - Restaurant Catalog" \
  --folder "03 - Full Order Flow [E2E]" \
  --folder "04 - Additional Endpoints"
```
