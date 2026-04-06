# ShieldAPI - Funcionalidades e Execucao Local

## Visao geral
ShieldAPI e uma API Java (Spring Boot 3) com foco em:

- autenticacao com JWT
- protecao de endpoints
- rate limiting distribuido com Redis
- documentacao via Swagger/OpenAPI

## Funcionalidades principais

### 1) Cadastro e login
- `POST /auth/register`: cria usuario com `username` e `password`
- `POST /auth/login`: autentica e retorna token JWT

### 2) Endpoint protegido
- `GET /api/test`: exige `Authorization: Bearer <token>`
- sem token valido, retorna bloqueio de acesso

### 3) Rate limiting
- aplica limite de requisicoes por identidade (usuario autenticado) ou IP
- configuracoes padrao:
  - `RATE_LIMIT=5`
  - `RATE_LIMIT_WINDOW_SECONDS=60`
  - `RATE_LIMIT_BAN_THRESHOLD=3`
  - `RATE_LIMIT_BAN_SECONDS=300`

### 4) Swagger
- UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Variaveis de ambiente usadas

Arquivo base: `.env.example`

- banco:
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
- redis:
  - `REDIS_HOST`
  - `REDIS_PORT`
- jwt:
  - `JWT_SECRET`
  - `JWT_EXPIRATION_MS`
- rate limit:
  - `RATE_LIMIT`
  - `RATE_LIMIT_WINDOW_SECONDS`
  - `RATE_LIMIT_BAN_THRESHOLD`
  - `RATE_LIMIT_BAN_SECONDS`

Importante:
- nao commitar `.env`
- use segredo forte para `JWT_SECRET` (minimo 32 caracteres)

## Como rodar localmente (sem Docker)

### Pre-requisitos
- Java 17+
- Maven 3.9+
- PostgreSQL ativo na maquina
- Redis ativo na maquina

### Passos
1. Criar `.env` a partir do exemplo

PowerShell:

```powershell
Copy-Item .env.example .env
```

2. Ajustar `.env` para ambiente local (host da maquina)

Exemplo minimo:

```env
DB_URL=jdbc:postgresql://localhost:5432/shieldapi
DB_USERNAME=shield
DB_PASSWORD=CHANGE_ME_DB_PASSWORD
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=CHANGE_ME_WITH_A_LONG_RANDOM_SECRET_AT_LEAST_32_CHARS
JWT_EXPIRATION_MS=3600000
RATE_LIMIT=5
RATE_LIMIT_WINDOW_SECONDS=60
RATE_LIMIT_BAN_THRESHOLD=3
RATE_LIMIT_BAN_SECONDS=300
```

3. Garantir que o usuario/senha do Postgres batem com `DB_USERNAME` e `DB_PASSWORD`

4. Subir aplicacao

```powershell
mvn spring-boot:run
```

5. Validar

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`

## Como rodar localmente com Docker

1. Criar `.env`

```powershell
Copy-Item .env.example .env
```

2. Subir stack

```powershell
docker compose --env-file .env up -d --build
```

3. Verificar servicos

```powershell
docker compose ps
```

4. Parar stack

```powershell
docker compose down
```

## Smoke test rapido

```powershell
$u = "user$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
$p = "SenhaForte123"
$payload = @{ username = $u; password = $p } | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/auth/register" -Method POST -ContentType "application/json" -Body $payload | Out-Null
$login = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method POST -ContentType "application/json" -Body $payload
$token = $login.token

Invoke-RestMethod -Uri "http://localhost:8080/api/test" -Headers @{ Authorization = "Bearer $token" }
```
