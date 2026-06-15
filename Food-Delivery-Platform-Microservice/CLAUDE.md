# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Services and Ports

| Service | Port | Description |
|---|---|---|
| Service Registry (Eureka) | 8761 | Must start first |
| API Gateway | 8080 | Single entry point; JWT validation here |
| Customer Service | 8081 | Auth, registration, customer profiles |
| Restaurant Service | 8082 | Restaurants, menus |
| Order Service | 8083 | Order placement and tracking; requires PostgreSQL + RabbitMQ |
| Delivery Service | 8084 | Delivery assignment; requires RabbitMQ |

## Build & Run

Each service is a standalone Maven project with a wrapper. Run from within each service directory:

```bash
# Build
./mvnw clean install        # Linux/Mac
mvnw.cmd clean install      # Windows

# Run
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=CustomerServiceTest

# Run a single test method
./mvnw test -Dtest=CustomerServiceTest#testRegister
```

**Startup order matters:** Eureka → API Gateway → Customer/Restaurant → Order → Delivery

External dependencies required before starting Order and Delivery services:
- PostgreSQL on `localhost:5432`, database `order_db`, credentials `postgres:rwanda`
- RabbitMQ on `localhost:5672`, credentials `guest:guest`

## Architecture

Spring Boot 3.4.5 / Java 21 / Spring Cloud 2024.0.1 microservices.

**Request flow:**  
Client → API Gateway (JWT validation + header injection) → Eureka load-balanced routing → target service

**Service communication:**
- *Synchronous:* OpenFeign clients with Eureka-based load balancing
  - Order Service calls Customer Service and Restaurant Service
  - Restaurant Service calls Customer Service
- *Asynchronous:* RabbitMQ topic exchange (`order.exchange`)
  - Order Service publishes `OrderPlacedEvent` (key: `order.placed`) and `OrderCancelledEvent` (key: `order.cancelled`)
  - Delivery Service listens to `order.placed.queue` and auto-creates a delivery record on every new order

**Databases:** Each service owns its own database (database-per-service). Customer, Restaurant, and Delivery use H2 in-memory (`create-drop`). Order uses PostgreSQL.

## Security Model

JWT validation is centralized in the API Gateway (`JwtAuthenticationFilter`). Services do **not** re-validate JWTs; they trust headers injected by the gateway:
- `X-Username` — authenticated username
- `X-User-Role` — role from JWT claims
- `X-Customer-Id` — extracted customer identifier

Each service has a `GatewayHeaderFilter` that reads these headers and builds a `SecurityContext`. Do not add JWT validation logic to individual services.

Public paths (no auth required): `/api/auth/register`, `/api/auth/login`, `/api/restaurants/search/**`, `/api/restaurants/*/menu`, `/actuator/**`

JWT secret is currently hardcoded across all `application.yml` files as the same value — any change must be updated in all five components (gateway + four services).

## Key Configuration Files

- `infrastructure/api-gateway/src/main/resources/application.yml` — route definitions and JWT secret
- `infrastructure/service-registry/src/main/resources/application.yml` — Eureka server config
- Each service's `src/main/resources/application.yml` — port, datasource, Eureka URL, RabbitMQ host

## Package Naming Inconsistency

Customer and Order services use `com.fooddelivery.*`; Restaurant and Delivery services use `com.david.*`. This is intentional in the current codebase — do not refactor without updating all cross-service Feign client configurations.
