# API Contracts

All requests go through the API Gateway at `http://localhost:8080`.

Authenticated endpoints require `Authorization: Bearer <jwt>` obtained from `POST /api/auth/login`.

---

## Customer Service

### POST /api/auth/register
Register a new customer account.

**Auth:** None

**Request body:**
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "secret123",
  "firstName": "Alice",
  "lastName": "Smith",
  "phone": "+250780000001",
  "deliveryAddress": "123 Main St",
  "city": "Kigali"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `username` | string | yes | non-blank |
| `email` | string | yes | valid email |
| `password` | string | yes | min 6 characters |
| `firstName` | string | no | — |
| `lastName` | string | no | — |
| `phone` | string | no | — |
| `deliveryAddress` | string | no | — |
| `city` | string | no | — |

**Response: 201 Created**
```json
{
  "token": "<jwt>",
  "customerId": 1,
  "username": "alice",
  "role": "CUSTOMER"
}
```

---

### POST /api/auth/login
Authenticate and receive a JWT.

**Auth:** None

**Request body:**
```json
{
  "username": "alice",
  "password": "secret123"
}
```

**Response: 200 OK**
```json
{
  "token": "<jwt>",
  "customerId": 1,
  "username": "alice",
  "role": "CUSTOMER"
}
```

---

### GET /api/customers/me
Get the authenticated customer's profile.

**Auth:** Required

**Response: 200 OK**
```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "firstName": "Alice",
  "lastName": "Smith",
  "phone": "+250780000001",
  "deliveryAddress": "123 Main St",
  "city": "Kigali",
  "role": "CUSTOMER"
}
```

Roles: `CUSTOMER`, `RESTAURANT_OWNER`, `DELIVERY_PARTNER`, `ADMIN`

---

### GET /api/customers/internal/{id}
Fetch a customer by ID. Called by Order Service via Feign.

**Auth:** Not enforced on this path (internal service call)

**Response: 200 OK** — same schema as `GET /api/customers/me`

**404** if the customer does not exist.

---

### PATCH /api/customers/internal/{username}/promote-to-owner
Promote a customer to `RESTAURANT_OWNER`. Called by Restaurant Service when creating a restaurant.

**Auth:** Not enforced on this path (internal service call)

**Response: 204 No Content**

---

## Restaurant Service

### GET /api/restaurants/search/city/{city}
List all active restaurants in a city.

**Auth:** None

**Response: 200 OK**
```json
[
  {
    "id": 1,
    "name": "Kigali Bites",
    "description": "Local favourites",
    "cuisineType": "RWANDAN",
    "address": "KN 5 Ave",
    "city": "Kigali",
    "phone": "+250780000002",
    "active": true,
    "rating": 4.5,
    "estimatedDeliveryMinutes": 30,
    "ownerUsername": "bob",
    "createdAt": "2025-01-15T10:00:00"
  }
]
```

---

### GET /api/restaurants/search/cuisine/{type}
List active restaurants by cuisine type (case-insensitive match).

**Auth:** None

**Response: 200 OK** — array of restaurant objects (same schema as above)

---

### GET /api/restaurants/search/all
List all active restaurants.

**Auth:** None

**Response: 200 OK** — array of restaurant objects

---

### GET /api/restaurants/{id}
Get a restaurant by ID.

**Auth:** None

**Response: 200 OK** — single restaurant object. **404** if not found.

---

### GET /api/restaurants/{id}/menu
Get all menu items for a restaurant.

**Auth:** None

**Response: 200 OK**
```json
[
  {
    "id": 1,
    "name": "Isombe",
    "description": "Cassava leaves with vegetables",
    "price": 4.50,
    "category": "MAIN",
    "available": true,
    "imageUrl": "https://example.com/isombe.jpg",
    "restaurantId": 1
  }
]
```

---

### POST /api/restaurants
Create a restaurant. The authenticated user is automatically registered as its owner. Their role is promoted to `RESTAURANT_OWNER` via an internal Feign call.

**Auth:** Required

**Request body:**
```json
{
  "name": "Kigali Bites",
  "description": "Local favourites",
  "cuisineType": "RWANDAN",
  "address": "KN 5 Ave",
  "city": "Kigali",
  "phone": "+250780000002",
  "estimatedDeliveryMinutes": 30
}
```

| Field | Type | Required |
|---|---|---|
| `name` | string | yes |
| `address` | string | yes |
| `city` | string | yes |
| `description` | string | no |
| `cuisineType` | string | no |
| `phone` | string | no |
| `estimatedDeliveryMinutes` | int | no |

**Response: 201 Created** — restaurant object

---

### POST /api/restaurants/{restaurantId}/menu
Add a menu item to a restaurant. The authenticated user must be the restaurant owner.

**Auth:** Required (must be `RESTAURANT_OWNER` of this restaurant)

**Request body:**
```json
{
  "name": "Isombe",
  "description": "Cassava leaves with vegetables",
  "price": 4.50,
  "category": "MAIN",
  "imageUrl": "https://example.com/isombe.jpg"
}
```

| Field | Type | Required |
|---|---|---|
| `name` | string | yes |
| `price` | decimal | yes |
| `description` | string | no |
| `category` | string | no |
| `imageUrl` | string | no |

**Response: 201 Created** — menu item object

---

### PUT /api/restaurants/menu/{itemId}
Update a menu item. The authenticated user must own the restaurant.

**Auth:** Required

**Request body:** Same as `POST .../menu`

**Response: 200 OK** — updated menu item object

---

### PATCH /api/restaurants/menu/{itemId}/toggle
Toggle a menu item's availability.

**Auth:** Required

**Response: 204 No Content**

---

## Order Service

### POST /api/orders
Place a new order. Rate-limited at 20 requests per second per gateway instance.

**Auth:** Required  
**Gateway header injected:** `X-Customer-Id`

**Request body:**
```json
{
  "restaurantId": 1,
  "items": [
    {
      "menuItemId": 3,
      "quantity": 2,
      "specialInstructions": "No chilli"
    }
  ],
  "deliveryAddress": "123 Main St",
  "specialInstructions": "Ring the bell"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `restaurantId` | long | yes | — |
| `items` | array | yes | non-empty |
| `items[].menuItemId` | long | yes | — |
| `items[].quantity` | int | yes | positive |
| `items[].specialInstructions` | string | no | — |
| `deliveryAddress` | string | no | — |
| `specialInstructions` | string | no | — |

**Response: 201 Created**
```json
{
  "id": 7,
  "status": "PLACED",
  "totalAmount": 11.99,
  "deliveryFee": 2.99,
  "deliveryAddress": "123 Main St",
  "specialInstructions": "Ring the bell",
  "createdAt": "2025-06-15T14:30:00",
  "estimatedDeliveryTime": null,
  "customerId": 42,
  "restaurantId": 1,
  "customerName": "alice",
  "restaurantName": "Kigali Bites",
  "items": [
    {
      "id": 1,
      "menuItemId": 3,
      "itemName": "Isombe",
      "quantity": 2,
      "unitPrice": 4.50,
      "subtotal": 9.00,
      "specialInstructions": "No chilli"
    }
  ]
}
```

Order statuses: `PLACED`, `CONFIRMED`, `PREPARING`, `READY_FOR_PICKUP`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`

---

### GET /api/orders/{id}
Get an order by ID.

**Auth:** Required

**Response: 200 OK** — order object. **404** if not found.

---

### GET /api/orders/my-orders
List all orders for the authenticated customer.

**Auth:** Required  
**Gateway header injected:** `X-Customer-Id`

**Response: 200 OK** — array of order objects

---

### GET /api/orders/restaurant/{restaurantId}
List all orders for a restaurant.

**Auth:** Required

**Response: 200 OK** — array of order objects

---

### PATCH /api/orders/{id}/status
Update an order's status.

**Auth:** Required

**Query param:** `status` — one of the `OrderStatus` enum values

**Response: 200 OK** — updated order object

---

### POST /api/orders/{id}/cancel
Cancel an order. Only the order's customer may cancel.

**Auth:** Required  
**Gateway header injected:** `X-Customer-Id`

**Response: 200 OK** — updated order object with `status: "CANCELLED"`

---

## Delivery Service

### GET /api/deliveries/{id}
Get a delivery by its ID.

**Auth:** Required

**Response: 200 OK**
```json
{
  "id": 3,
  "status": "ASSIGNED",
  "orderId": 7,
  "customerUsername": "alice",
  "driverName": "David",
  "driverPhone": "+250780000099",
  "pickupAddress": "KN 5 Ave",
  "deliveryAddress": "123 Main St",
  "createdAt": "2025-06-15T14:30:05",
  "assignedAt": "2025-06-15T14:35:00",
  "pickedUpAt": null,
  "deliveredAt": null
}
```

Delivery statuses: `PENDING`, `ASSIGNED`, `PICKED_UP`, `DELIVERED`, `CANCELLED`

---

### GET /api/deliveries/order/{orderId}
Get the delivery record for a given order.

**Auth:** Required

**Response: 200 OK** — delivery object. **404** if no delivery exists for that order.

---

### GET /api/deliveries?status={status}
List deliveries filtered by status.

**Auth:** Required

**Query param:** `status` — delivery status value

**Response: 200 OK** — array of delivery objects

---

### GET /api/deliveries/my-deliveries
List deliveries assigned to the authenticated driver.

**Auth:** Required

**Response: 200 OK** — array of delivery objects

---

### PATCH /api/deliveries/{id}/status
Update a delivery's status. Transitions to `PICKED_UP` or `DELIVERED` publish a `DeliveryStatusUpdatedEvent` to `delivery.exchange`.

**Auth:** Required

**Query param:** `status` — delivery status value

**Response: 200 OK** — updated delivery object

---

### POST /api/deliveries/order/{orderId}/cancel
Cancel the delivery for a given order.

**Auth:** Required

**Response: 200 OK** — updated delivery object with `status: "CANCELLED"`

---

## Error Responses

All services return errors in a consistent envelope:

```json
{
  "timestamp": "2025-06-15T14:30:00",
  "status": 404,
  "message": "Order not found: 99"
}
```

Validation errors include a field-level breakdown:

```json
{
  "timestamp": "2025-06-15T14:30:00",
  "status": 400,
  "errors": {
    "restaurantId": "must not be null",
    "items": "must not be empty"
  }
}
```

| HTTP Status | Cause |
|---|---|
| 400 | Validation failure or illegal state transition |
| 401 | Missing or invalid JWT |
| 403 | Authenticated but not authorized for this resource |
| 404 | Resource not found |
| 409 | Duplicate resource (e.g., username already taken) |
| 429 | Rate limit exceeded (POST /api/orders only) |
| 503 | Upstream service unavailable or circuit breaker open |

When a circuit breaker is open, the 503 body includes the breaker name:

```json
{
  "error": "Service temporarily unavailable (circuit breaker open): customer-service"
}
```
