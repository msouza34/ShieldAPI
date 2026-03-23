# Operations Runbook

## Docker Commands

Start all services:

```bash
docker compose up -d --build
```

Check status:

```bash
docker compose ps
```

Show application logs:

```bash
docker compose logs -f app
```

Stop all services:

```bash
docker compose down
```

Stop and remove volumes (danger: removes local DB data):

```bash
docker compose down -v
```

## Health Checks

API OpenAPI endpoint:

```bash
curl -i http://localhost:8080/v3/api-docs
```

Expected: HTTP `200`.

## Common Issues

### Swagger not loading

Check:
- container is up: `docker compose ps`
- URL is correct: `http://localhost:8080/swagger-ui.html`

### `403` on `/api/test`

Cause:
- endpoint is protected and request is missing/invalid JWT

Fix:
- login at `/auth/login`
- send `Authorization: Bearer <JWT>`

### `429` responses

Cause:
- rate limit exceeded or identity temporarily banned

Fix options:
- wait for `Retry-After` seconds
- use another authenticated identity
- adjust `RATE_LIMIT*` variables if policy must be changed

### Ban persists during tests

Because counters live in Redis, bans survive between API restarts.

To reset only Redis state:

```bash
docker compose restart redis
```

To reset all services and volumes:

```bash
docker compose down -v && docker compose up -d --build
```

## Logs and Audit

Blocked requests are logged with marker `request_blocked` and include:
- reason (`RATE_LIMIT_EXCEEDED` or `TEMPORARY_BAN`)
- identity
- ip
- path
- method
- timestamp

Filter logs quickly:

```bash
docker compose logs app --tail 200 | grep request_blocked
```

## Recommended Production Ops

- run behind reverse proxy and forward client IP headers
- restrict direct DB/Redis network exposure
- use secret management for `JWT_SECRET`
- configure centralized logging and retention
- monitor rate-limit block ratio and auth failures
