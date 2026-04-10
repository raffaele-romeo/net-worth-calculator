# API Reference

Base URL: `http://localhost:9000/v1`

All secured endpoints require the header: `Authorization: Bearer <jwt_token>`

---

## Health

### GET /healthcheck

Check if services are running.

**Auth:** None

**Response:** `200 OK`
```json
{
  "redis": true,
  "postgres": true
}
```

---

## Authentication

### POST /auth/users

Register a new user.

**Auth:** None

**Request:**
```json
{
  "username": "john",
  "password": "secret123"
}
```

**Response:** `201 Created`
```json
"eyJhbGciOiJIUzI1NiJ9..."
```

**Errors:**
- `400 Bad Request` — validation errors (e.g. empty username/password)
- `409 Conflict` — username already taken

---

### POST /auth/login

Log in with existing credentials.

**Auth:** None

**Request:**
```json
{
  "username": "john",
  "password": "secret123"
}
```

**Response:** `200 OK`
```json
"eyJhbGciOiJIUzI1NiJ9..."
```

**Errors:**
- `400 Bad Request` — validation errors
- `403 Forbidden` — wrong username or password

---

### POST /auth/logout

Log out (invalidates the JWT token in Redis).

**Auth:** Required

**Request:** Empty body

**Response:** `204 No Content`

---

## Assets

### GET /assets

List all assets for the authenticated user.

**Auth:** Required

**Response:** `200 OK`
```json
[
  {
    "assetId": 1,
    "assetType": "Cash",
    "assetName": "Savings Account",
    "userId": 1
  },
  {
    "assetId": 2,
    "assetType": "Investment",
    "assetName": "Stock Portfolio",
    "userId": 1
  }
]
```

---

### POST /assets

Create a new asset.

**Auth:** Required

**Request:**
```json
{
  "assetType": "Cash",
  "assetName": "Savings Account"
}
```

Valid asset types: `Loan`, `Cash`, `Investment`, `Property`

**Response:** `201 Created` (empty body)

**Errors:**
- `400 Bad Request` — invalid asset type or asset name already in use

---

### DELETE /assets/:id

Delete an asset by ID.

**Auth:** Required

**Response:** `204 No Content`

---

## Transactions

### GET /transactions

List all transactions for the authenticated user.

**Auth:** Required

**Response:** `200 OK`
```json
[
  {
    "totals": ["$ 1,500.00", "EUR 2,000.00"],
    "month": 3,
    "year": 2026
  }
]
```

Note: totals are formatted strings like `"$ 1,500.00"` (currency symbol + amount).

---

### POST /transactions

Create transactions for a given month/year.

**Auth:** Required

**Request:**
```json
{
  "month": 3,
  "year": 2026,
  "transactions": [
    {
      "amount": 1500.00,
      "currency": "USD",
      "assetId": 1
    },
    {
      "amount": 2000.00,
      "currency": "EUR",
      "assetId": 2
    }
  ]
}
```

**Response:** `201 Created` (empty body)

**Errors:**
- `400 Bad Request` — validation errors or transaction already exists for that month/year/asset

---

### DELETE /transactions/:id

Delete a transaction by ID.

**Auth:** Required

**Response:** `204 No Content`

---

### GET /transactions/net-worth?year=2026&currency=USD

Calculate total net worth across all assets.

**Auth:** Required

**Query parameters (both optional):**
- `year` — filter by year (e.g. `2026`)
- `currency` — convert all totals to a single currency (e.g. `USD`, `EUR`)

**Response:** `200 OK`
```json
[
  {
    "totals": ["$ 3,500.00"],
    "month": 3,
    "year": 2026
  }
]
```

**Errors:**
- `400 Bad Request` — invalid year or currency parameter
- `503 Service Unavailable` — currency conversion service failed

---

### GET /assets/:id/transactions?year=2026&currency=USD

Get transactions for a specific asset by asset ID.

**Auth:** Required

**Query parameters (both optional):**
- `year` — filter by year
- `currency` — convert totals to a single currency

**Response:** `200 OK`
```json
[
  {
    "totals": ["$ 1,500.00"],
    "month": 3,
    "year": 2026
  }
]
```

**Errors:**
- `400 Bad Request` — invalid query parameters
- `503 Service Unavailable` — currency conversion service failed

---

### GET /assets/:assetType/transactions?year=2026&currency=USD

Get transactions for all assets of a given type (e.g. `Cash`, `Investment`).

**Auth:** Required

**Query parameters (both optional):**
- `year` — filter by year
- `currency` — convert totals to a single currency

**Response:** `200 OK`
```json
[
  {
    "totals": ["$ 1,500.00"],
    "month": 3,
    "year": 2026
  }
]
```

**Errors:**
- `400 Bad Request` — invalid query parameters
- `503 Service Unavailable` — currency conversion service failed
