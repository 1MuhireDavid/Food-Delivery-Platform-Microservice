# Food Delivery Platform

A microservices-based food delivery platform built with Spring Boot 3.4.5, Java 21, and Spring Cloud 2024.0.1.

## Architecture

<img width="2720" height="3280" alt="improved_microservices_architecture" src="https://github.com/user-attachments/assets/8b037e14-4386-40f7-b802-0fae372366cf" />


See [docs/architecture.md](docs/architecture.md) for the full component diagram, request flow walkthrough, and security model.

---

## Services

| Service | Port | Responsibilities |
|---|---|---|
| Service Registry | 8761 | Eureka server — all services register here |
| API Gateway | 8080 | JWT validation, rate limiting, Eureka-based routing |
| Customer Service | 8081 | Registration, JWT login, customer profiles |
| Restaurant Service | 8082 | Restaurant CRUD, menu management |
| Order Service | 8083 | Order placement, status tracking, event publishing |
| Delivery Service | 8084 | Delivery assignment via events, driver status updates |

---

## Prerequisites

**Docker (recommended):** Docker 24+ and Docker Compose v2.

**Local development:** Java 21, Maven 3.9+, PostgreSQL 15, RabbitMQ 3.12.

---

## Quick Start — Docker

```bash
# 1. Copy and fill in credentials
cp .env.example .env
# Required: set POSTGRES_PASSWORD and JWT_SECRET

# 2. Build images and start all services
docker compose up --build
```

Containers start in dependency order enforced by health checks:

```
postgres + rabbitmq → eureka → api-gateway → customer + restaurant → order → delivery
```

Allow ~60 seconds for all JVMs to start. Then:

| Endpoint | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| RabbitMQ Management | http://localhost:15672 |

---

## Local Development (no Docker)

Start external dependencies first:

```bash
# PostgreSQL — Order Service is the only one that needs a real DB in dev
psql -U postgres -c "CREATE DATABASE order_db;"

# RabbitMQ — default guest:guest credentials are fine for local dev
rabbitmq-server
```

Start services in order (each command runs from within its own directory):

```bash
# 1
cd infrastructure/service-registry  && ./mvnw spring-boot:run   # Windows: mvnw.cmd

# 2
cd infrastructure/api-gateway       && ./mvnw spring-boot:run

# 3 — these two can start in parallel
cd services/customer-service        && ./mvnw spring-boot:run
cd services/restaurant-service      && ./mvnw spring-boot:run

# 4
cd services/order-service           && ./mvnw spring-boot:run

# 5
cd services/delivery-service        && ./mvnw spring-boot:run
```

---

## Environment Variables

Copy `.env.example` to `.env` and set the following before running Docker:

| Variable | Default | Notes |
|---|---|---|
| `POSTGRES_USER` | `postgres` | PostgreSQL superuser name |
| `POSTGRES_PASSWORD` | — | **Required.** No default. |
| `RABBITMQ_USER` | `guest` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `JWT_SECRET` | (dev key) | HS256 signing key — must match across all 5 components |

The JWT secret is shared among the API Gateway and all four services. Update it in every `application.yml` under `app.jwt.secret`, or rely on the `JWT_SECRET` environment variable when using Docker.

---

## Running Tests

```bash
cd services/<service-name>

./mvnw test                                     # all tests
./mvnw test -Dtest=CustomerServiceTest          # single class
./mvnw test -Dtest=CustomerServiceTest#testRegister  # single method
```

---

## Documentation

| Document | Contents |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Component diagram, request flow, sync/async communication, security model |
| [docs/api-contracts.md](docs/api-contracts.md) | Every endpoint with request/response schemas and error codes |
| [docs/migration-decisions.md](docs/migration-decisions.md) | Rationale for each service boundary and technology choice |
