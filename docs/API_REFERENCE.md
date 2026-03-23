# API Reference

Base URL:
- `http://localhost:8080`

OpenAPI:
- JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui.html`

## Authentication

### Register user

- Method: `POST`
- Path: `/auth/register`
- Auth: none

Request body:

```json
{
  "username": "matheus",
  "password": "Pass12345"
}
```

Validation rules:
- `username`: 3..50 chars, alphanumeric plus `.`, `_`, `-`
- `password`: 6..100 chars

Success response (`201`):

```json
{
  "message": "User registered successfully"
}
```

Error responses:
- `400`: validation error
- `409`: username already exists
- `429`: rate limit exceeded

### Login

- Method: `POST`
- Path: `/auth/login`
- Auth: none

Request body:

```json
{
  "username": "matheus",
  "password": "Pass12345"
}
```

Success response (`200`):

```json
{
  "token": "<JWT>",
  "tokenType": "Bearer",
  "username": "matheus",
  "expiresInMs": 3600000
}
```

Error responses:
- `400`: validation error
- `401`: invalid credentials
- `429`: rate limit exceeded

## Protected Endpoint

### Test endpoint

- Method: `GET`
- Path: `/api/test`
- Auth: `Bearer <JWT>`

Request header:

```http
Authorization: Bearer <JWT>
```

Success response (`200`):

```json
{
  "message": "Protected endpoint reached by matheus at 2026-03-23T02:00:30.419489242Z"
}
```

Common error responses:
- `403`: missing or invalid token (current behavior)
- `429`: rate limit exceeded or temporary ban

Sample rate-limited response (`429`):

Headers:

```http
Retry-After: 59
Content-Type: application/json;charset=UTF-8
```

Body:

```json
{
  "timestamp": "2026-03-23T01:24:22.534668088Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded",
  "identity": "ip:203.0.113.60",
  "path": "/api/test"
}
```

## Utility Routes

- `GET /` -> redirect to `/swagger-ui.html`
- `GET /swagger-ui.html` -> Swagger UI
- `GET /v3/api-docs` -> OpenAPI JSON

## cURL Examples

Register:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"matheus","password":"Pass12345"}'
```

Login:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"matheus","password":"Pass12345"}'
```

Protected request:

```bash
curl -X GET http://localhost:8080/api/test \
  -H "Authorization: Bearer <JWT>"
```
