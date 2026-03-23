# Rate Limiting Internals

## Goal

Protect endpoints against abuse using a distributed strategy that works across multiple API instances.

## Algorithm

ShieldAPI uses Sliding Window with Redis.

Implementation details:
- Data structure: Redis `ZSET`
- Script engine: Lua script executed atomically
- Key space:
  - `rl:window:{identity}`: sliding window timestamps
  - `rl:violations:{identity}`: consecutive violation counter
  - `rl:ban:{identity}`: temporary ban marker

## Identity Resolution

The filter resolves an identity in this order:

1. If `Authorization: Bearer <JWT>` is present and valid:
- identity = `user:{username}`

2. Otherwise:
- identity = `ip:{ip}`

IP extraction priority:
- `X-Forwarded-For` first item
- `X-Real-IP`
- `request.getRemoteAddr()`

## Default Policy

- `limit = 5`
- `windowSeconds = 60`
- `banThreshold = 3` (consecutive over-limit events)
- `banSeconds = 300`

## Request Lifecycle

For each request (except excluded routes):

1. Check if `rl:ban:{identity}` exists
- if yes -> return `429` with `Retry-After` using key TTL

2. Run Lua script for sliding window:
- remove outdated scores (`now - window`)
- count active scores
- if count >= limit -> deny
- else add current request and allow

3. If denied:
- increment `rl:violations:{identity}`
- if violations >= threshold -> set ban key and clear violation counter

4. If allowed:
- clear `rl:violations:{identity}` to enforce consecutive semantics

## Response Contract When Blocked

- Status: `429 Too Many Requests`
- Header: `Retry-After: <seconds>`
- Body fields:
  - `timestamp`
  - `status`
  - `error`
  - `message`
  - `identity`
  - `path`

## Excluded Routes

The following routes are excluded from rate limiting:
- `/`
- `/favicon.ico`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/error`

## Tuning Recommendations

For production:
- tune limits per endpoint group (auth vs business endpoints)
- consider adding method + path to key when needed
- monitor block rate and false positives
- place API behind a trusted reverse proxy and preserve real client IP
