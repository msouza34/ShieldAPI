# ShieldAPI — Documentação Técnica Completa

**Versão:** 2.0  
**Autor:** Matheus  
**Data:** Março de 2026  
**Status:** Produção  
**Diagramas:** Inclusos

---

## 📋 Sumário Executivo

ShieldAPI é uma solução de backend robusta e escalável desenvolvida com **Java 17** e **Spring Boot 3**, especializada em proteção distribuída de endpoints através de autenticação JWT e rate limiting inteligente com Redis. O projeto implementa um modelo de segurança em camadas que combina autenticação, controle de acesso e mitigação de abuso em uma única plataforma coesa.

A arquitetura foi projetada para ambientes de produção, oferecendo resiliência, observabilidade e conformidade com práticas de segurança moderna. O sistema é containerizado via Docker Compose, facilitando deploy em qualquer infraestrutura.

---

## 📑 Índice

1. [Visão Geral da Solução](#1-visão-geral-da-solução)
2. [Arquitetura do Sistema](#2-arquitetura-do-sistema)
3. [Endpoints da API](#3-endpoints-da-api)
4. [Rate Limiting — Internals](#4-rate-limiting--internals)
5. [Configuração e Deployment](#5-configuração-e-deployment)
6. [Testes](#6-testes)
7. [Observabilidade e Monitoramento](#7-observabilidade-e-monitoramento)
8. [Segurança — Boas Práticas](#8-segurança--boas-práticas)
9. [Troubleshooting](#9-troubleshooting)
10. [Roadmap e Melhorias Futuras](#10-roadmap-e-melhorias-futuras)
11. [Referências e Recursos](#11-referências-e-recursos)
12. [Suporte e Contribuição](#12-suporte-e-contribuição)

---

## 1. Visão Geral da Solução

### 1.1 Objetivo e Escopo

ShieldAPI resolve o desafio crítico de proteger endpoints de API contra abusos, ataques de força bruta e consumo excessivo de recursos. O sistema implementa três camadas de proteção:

1. **Autenticação**: Validação de identidade via JWT (JSON Web Tokens)
2. **Autorização**: Controle de acesso baseado em identidade (RBAC ready)
3. **Rate Limiting**: Limitação distribuída de requisições com Redis, algoritmo Sliding Window

### 1.2 Stack Tecnológico

| Componente | Tecnologia | Versão | Propósito |
|---|---|---|---|
| **Runtime** | Java | 17+ | Linguagem de programação |
| **Framework Web** | Spring Boot | 3.x | Framework MVC e REST |
| **Segurança** | Spring Security | 6.x | Autenticação e autorização |
| **Autenticação** | JWT (jjwt) | 0.12+ | Tokens sem estado |
| **Banco de Dados** | PostgreSQL | 14+ | Persistência de usuários |
| **Cache/Rate Limiting** | Redis | 7.x | Sliding window distribuído |
| **Build** | Maven | 3.9+ | Gerenciamento de dependências |
| **Containerização** | Docker | 20.10+ | Orquestração de serviços |
| **Documentação API** | Swagger/OpenAPI | 3.0 | Especificação e UI interativa |

### 1.3 Características Principais

**Autenticação e Autorização:**
- Registro de usuários com validação de entrada
- Login com emissão de JWT com expiração configurável
- Endpoints protegidos com validação de token
- Estrutura pronta para RBAC (Role-Based Access Control)

**Rate Limiting Distribuído:**
- Algoritmo Sliding Window implementado via Redis ZSET
- Identificação por usuário (quando autenticado) ou IP (quando anônimo)
- Limite configurável: padrão 5 requisições por 60 segundos
- Ban temporário automático: 3 violações consecutivas = 5 minutos de bloqueio
- Resposta HTTP 429 (Too Many Requests) com header `Retry-After`

**Observabilidade:**
- Logs estruturados em JSON para requisições bloqueadas
- Campos contextuais: motivo, identidade, IP, caminho, método, timestamp
- Integração com sistemas de logging centralizados (ELK, Splunk, etc.)

**Documentação Interativa:**
- Swagger UI integrada em `/swagger-ui.html`
- OpenAPI 3.0 em `/v3/api-docs`
- Endpoints exploráveis e testáveis via UI

---

## 2. Arquitetura do Sistema

### 2.1 Diagrama de Fluxo de Requisição

O diagrama abaixo ilustra o fluxo completo de processamento de uma requisição HTTP no ShieldAPI, desde a chegada até a resposta final. Este fluxo mostra todos os pontos de verificação de segurança, validações e processamento de dados:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FLUXO DE REQUISIÇÃO SHIELDAPI                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. Requisição HTTP Chega                                                  │
│     ↓                                                                       │
│  2. API Gateway (NGINX Reverse Proxy + TLS/SSL)                            │
│     ↓                                                                       │
│  3. CORS Handler (Validar origem da requisição)                            │
│     ↓                                                                       │
│  4. RateLimitingFilter (Redis Sliding Window)                              │
│     ├─ Verificar se identidade está banida                                 │
│     ├─ Limpar timestamps antigos (fora da janela)                          │
│     ├─ Contar requisições na janela                                        │
│     ├─ Se limite excedido: incrementar violações                           │
│     ├─ Se 3 violações: ativar ban por 5 minutos                            │
│     └─ Se OK: adicionar novo timestamp ao Redis                            │
│     ↓                                                                       │
│  5. JwtAuthenticationFilter (Validação de Token)                           │
│     ├─ Extrair Bearer Token do header Authorization                        │
│     ├─ Validar assinatura JWT                                              │
│     └─ Carregar contexto de segurança com claims do usuário                │
│     ↓                                                                       │
│  6. AuthorizationFilter (Verificação de Permissões)                        │
│     ├─ Validar se usuário tem permissão para o endpoint                    │
│     └─ Verificar roles e authorities                                       │
│     ↓                                                                       │
│  7. REST Controller (AuthController / ApiController)                       │
│     ├─ Receber requisição autenticada e autorizada                         │
│     └─ Rotear para método apropriado                                       │
│     ↓                                                                       │
│  8. Business Logic Layer (Services)                                        │
│     ├─ AuthService (Login/Register)                                        │
│     ├─ UserService (CRUD de usuários)                                      │
│     ├─ JwtTokenProvider (Geração/Validação de tokens)                      │
│     └─ RateLimitService (Lógica de rate limiting)                          │
│     ↓                                                                       │
│  9. Data Access Layer (Repositories)                                       │
│     ├─ UserRepository (Spring Data JPA)                                    │
│     └─ AuditRepository (Logging de eventos)                                │
│     ↓                                                                       │
│  10. Persistence Layer                                                     │
│      ├─ PostgreSQL (Executar queries)                                      │
│      └─ Redis (Operações de cache/rate limiting)                           │
│     ↓                                                                       │
│  11. Response Processing                                                   │
│      ├─ Serializar dados para JSON                                         │
│      ├─ Adicionar headers HTTP apropriados                                 │
│      └─ Structured Logging (JSON logs)                                     │
│     ↓                                                                       │
│  12. Resposta HTTP (200 OK / 4xx / 5xx)                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Caminhos de Bloqueio (Requisições Rejeitadas):**

- **Ban Ativo**: 429 Too Many Requests (Retry-After: 300s)
- **Limite Excedido**: 429 Too Many Requests (Retry-After: 45s)
- **JWT Inválido/Expirado**: 401 Unauthorized
- **Permissões Insuficientes**: 403 Forbidden
- **Erro de Servidor**: 500 Internal Server Error

### 2.2 Diagrama de Arquitetura Completo

A arquitetura do ShieldAPI segue um padrão em camadas bem definido:

**Camadas (de cima para baixo):**

1. **CLIENT LAYER** — Web Browser, Mobile App, CLI/Tools
2. **GATEWAY & SECURITY** — Load Balancer, NGINX Reverse Proxy, WAF Protection
3. **SECURITY FILTERS** — RateLimitingFilter, JwtAuthenticationFilter, AuthorizationFilter
4. **REST CONTROLLERS** — AuthController, ApiController, SwaggerController
5. **BUSINESS LOGIC** — UserService, AuthService, JwtTokenProvider, RateLimitService
6. **DATA ACCESS** — UserRepository, AuditRepository (Spring Data JPA)
7. **PERSISTENCE** — PostgreSQL 14+, Redis 7+
8. **OBSERVABILITY** — Structured Logs, Prometheus Metrics, Distributed Tracing

### 2.3 Componentes Principais

**SecurityFilterChain:**
A cadeia de filtros de segurança processa cada requisição em ordem sequencial. O RateLimitingFilter é executado primeiro para bloquear requisições abusivas antes que cheguem aos controladores, economizando recursos.

**RateLimitingFilter:**
Implementa o algoritmo Sliding Window usando Redis ZSET. Para cada identidade (usuário ou IP), mantém um conjunto ordenado de timestamps de requisições. Quando uma nova requisição chega, remove entradas antigas (fora da janela de 60 segundos) e verifica se o limite foi excedido.

**JwtAuthenticationFilter:**
Extrai o token Bearer do header `Authorization`, valida a assinatura usando a chave secreta, e carrega o contexto de segurança com as claims do usuário.

**Controllers:**
Endpoints REST que processam requisições autenticadas. O controlador de autenticação é público (sem JWT obrigatório), enquanto o controlador de API requer autenticação válida.

**Services:**
Lógica de negócio isolada em camadas de serviço. UserService gerencia usuários, AuthService orquestra login/registro, JwtTokenProvider cria/valida tokens, e RateLimitService interage com Redis.

---

## 3. Endpoints da API

### 3.1 Mapa de Endpoints

| Método | Caminho | Autenticação | Rate Limited | Descrição |
|---|---|---|---|---|
| **POST** | `/auth/register` | Não | Sim | Registrar novo usuário |
| **POST** | `/auth/login` | Não | Sim | Autenticar e emitir JWT |
| **GET** | `/api/test` | Sim (JWT) | Sim | Endpoint protegido de teste |
| **GET** | `/swagger-ui.html` | Não | Não | Interface Swagger |
| **GET** | `/v3/api-docs` | Não | Não | Especificação OpenAPI JSON |
| **GET** | `/` | Não | Não | Redirecionamento para Swagger |

### 3.2 Detalhamento de Endpoints

#### 3.2.1 POST /auth/register

**Descrição:** Cria uma nova conta de usuário no sistema.

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "email": "john@example.com",
  "createdAt": "2026-03-06T10:30:00Z"
}
```

**Códigos de Status:**
- `201 Created`: Usuário registrado com sucesso
- `400 Bad Request`: Dados inválidos ou usuário já existe
- `429 Too Many Requests`: Limite de requisições excedido

**Rate Limiting:** Aplicável. Limite padrão: 5 registros por IP a cada 60 segundos.

---

#### 3.2.2 POST /auth/login

**Descrição:** Autentica um usuário e emite um JWT válido.

**Request Body:**
```json
{
  "username": "john_doe",
  "password": "SecurePass123!"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

**Códigos de Status:**
- `200 OK`: Login bem-sucedido
- `401 Unauthorized`: Credenciais inválidas
- `429 Too Many Requests`: Limite de requisições excedido

**Rate Limiting:** Aplicável. Limite padrão: 5 tentativas de login por IP a cada 60 segundos. Após 3 falhas consecutivas, IP é banido por 5 minutos.

---

#### 3.2.3 GET /api/test

**Descrição:** Endpoint protegido para validação de autenticação. Requer JWT válido no header `Authorization`.

**Request Header:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**
```json
{
  "message": "Acesso autorizado",
  "user": "john_doe",
  "timestamp": "2026-03-06T10:35:00Z"
}
```

**Códigos de Status:**
- `200 OK`: Requisição bem-sucedida
- `401 Unauthorized`: Token ausente ou inválido
- `403 Forbidden`: Token expirado ou permissões insuficientes
- `429 Too Many Requests`: Limite de requisições excedido

**Rate Limiting:** Aplicável. Limite padrão: 5 requisições por usuário autenticado a cada 60 segundos.

---

### 3.3 Tratamento de Erros

Todas as respostas de erro seguem um formato padronizado:

```json
{
  "error": "Too Many Requests",
  "message": "Limite de requisições excedido. Tente novamente em 45 segundos.",
  "status": 429,
  "timestamp": "2026-03-06T10:40:00Z",
  "path": "/api/test"
}
```

**Headers de Rate Limiting:**
- `Retry-After`: Segundos até a próxima tentativa permitida
- `X-RateLimit-Limit`: Limite de requisições na janela
- `X-RateLimit-Remaining`: Requisições restantes
- `X-RateLimit-Reset`: Timestamp Unix do reset

---

## 4. Rate Limiting — Internals

### 4.1 Algoritmo Sliding Window

O Sliding Window é um algoritmo de rate limiting que mantém uma janela móvel de tempo (padrão: 60 segundos). Para cada identidade, o sistema armazena timestamps de todas as requisições dentro dessa janela em um Redis ZSET.

**Pseudocódigo:**
```
função verificarRateLimit(identidade, limite, janela):
  agora = timestamp_atual()
  limite_inferior = agora - janela
  
  # Remove timestamps antigos
  redis.zremrangebyscore(identidade, 0, limite_inferior)
  
  # Conta requisições na janela
  count = redis.zcard(identidade)
  
  se count >= limite:
    incrementar_violacoes(identidade)
    se violacoes >= 3:
      banir(identidade, 300 segundos)
      retornar BLOQUEADO_BAN
    retornar BLOQUEADO_LIMITE
  
  # Adiciona novo timestamp
  redis.zadd(identidade, agora, agora)
  redis.expire(identidade, janela)
  
  retornar PERMITIDO
```

### 4.2 Fluxo de Rate Limiting Detalhado

O fluxo completo de processamento de uma requisição segue estas etapas:

1. **Verificação de Ban**: Consulta Redis para verificar se a identidade está banida
2. **Limpeza de Janela**: Remove timestamps antigos (fora da janela de 60s)
3. **Contagem de Requisições**: Verifica quantas requisições foram feitas na janela
4. **Decisão de Limite**: Se excedeu 5 requisições, incrementa violações
5. **Verificação de Violações**: Se 3 violações, ativa ban automático por 5 minutos
6. **Adição de Timestamp**: Se dentro do limite, adiciona novo timestamp ao Redis
7. **Validação JWT**: Verifica autenticidade e expiração do token
8. **Verificação de Autorização**: Valida permissões do usuário para o endpoint
9. **Processamento**: Se tudo OK, processa a requisição e retorna 200 OK

### 4.3 Identificação de Identidade

O sistema usa uma estratégia de fallback para identificar requisições:

1. **Usuário Autenticado**: Se o JWT é válido, usa `user:{username}` como chave
2. **Anônimo**: Se sem JWT ou token inválido, usa `ip:{ip_address}` como chave

Essa abordagem oferece proteção em duas frentes: usuários legítimos têm limite generoso por usuário, enquanto IPs anônimos têm limite mais restritivo.

### 4.4 Ban Temporário

Após 3 violações consecutivas de rate limit, o sistema ativa um ban temporário de 5 minutos (configurável). Durante o ban:

- Todas as requisições da identidade retornam `429 Too Many Requests`
- Nenhuma requisição é processada
- Contador de violações é resetado quando o ban expira

**Chave Redis para ban:** `ban:{identidade}:{timestamp_expiracao}`

### 4.5 Endpoints Excluídos

Os seguintes endpoints **não estão sujeitos** a rate limiting:

- `/` (raiz)
- `/favicon.ico`
- `/swagger-ui.html`
- `/swagger-ui/**` (todos os recursos Swagger)
- `/v3/api-docs/**` (documentação OpenAPI)
- `/error` (página de erro)

Essa exclusão evita que documentação e recursos estáticos sejam bloqueados, mantendo a API acessível mesmo sob ataque.

---

## 5. Configuração e Deployment

### 5.1 Variáveis de Ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `DB_URL` | Sim | `jdbc:postgresql://localhost:5432/shieldapi` | URL JDBC do PostgreSQL |
| `DB_USERNAME` | Sim | `shield` | Usuário do banco de dados |
| `DB_PASSWORD` | Sim | `shield` | Senha do banco de dados |
| `REDIS_HOST` | Sim | `localhost` | Host do Redis |
| `REDIS_PORT` | Não | `6379` | Porta do Redis |
| `JWT_SECRET` | Sim | (definido em `application.yml`) | Chave secreta para assinar JWT |
| `JWT_EXPIRATION_MS` | Não | `3600000` | Expiração do JWT em milissegundos (1 hora) |
| `RATE_LIMIT` | Não | `5` | Número de requisições permitidas por janela |
| `RATE_LIMIT_WINDOW_SECONDS` | Não | `60` | Duração da janela em segundos |
| `RATE_LIMIT_BAN_THRESHOLD` | Não | `3` | Violações consecutivas antes de ban |
| `RATE_LIMIT_BAN_SECONDS` | Não | `300` | Duração do ban em segundos (5 minutos) |

### 5.2 Arquivo .env.example

```bash
# PostgreSQL
DB_URL=jdbc:postgresql://postgres:5432/shieldapi
DB_USERNAME=shield
DB_PASSWORD=shield_secure_password_123

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# JWT
JWT_SECRET=your_super_secret_key_change_in_production_12345
JWT_EXPIRATION_MS=3600000

# Rate Limiting
RATE_LIMIT=5
RATE_LIMIT_WINDOW_SECONDS=60
RATE_LIMIT_BAN_THRESHOLD=3
RATE_LIMIT_BAN_SECONDS=300
```

### 5.3 Docker Compose

**Arquivo: `docker-compose.yml`**

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: shieldapi
      POSTGRES_USER: shield
      POSTGRES_PASSWORD: shield
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U shield"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  shieldapi:
    build: .
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/shieldapi
      DB_USERNAME: shield
      DB_PASSWORD: shield
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: ${JWT_SECRET:-default_secret_change_me}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy

volumes:
  postgres_data:
```

**Iniciar com Docker Compose:**

```bash
docker compose up --build
```

Após startup:
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### 5.4 Execução Local (sem Docker)

**Pré-requisitos:**
- Java 17+
- Maven 3.9+
- PostgreSQL 14+ rodando
- Redis 7+ rodando

**Passos:**

1. Clonar repositório:
```bash
git clone <repository-url>
cd shieldapi
```

2. Configurar variáveis de ambiente:
```bash
cp .env.example .env
# Editar .env com suas credenciais locais
```

3. Executar aplicação:
```bash
mvn spring-boot:run
```

4. Acessar:
- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`

---

## 6. Testes

### 6.1 Executar Suite de Testes

```bash
mvn test
```

### 6.2 Cobertura de Testes

A suite atual cobre:

- **RateLimiterServiceTest**: Fluxo permitido, fluxo de ban temporário, reset após expiração
- **JwtTokenProviderTest**: Geração de token, validação de assinatura, expiração
- **AuthServiceTest**: Registro de usuário, login com credenciais válidas/inválidas
- **IntegrationTests**: Fluxos end-to-end com PostgreSQL e Redis

### 6.3 Exemplo de Teste Unitário

```java
@Test
void testRateLimitAllowedRequest() {
  String identity = "user:john_doe";
  
  // Primeira requisição deve ser permitida
  RateLimitResult result = rateLimitService.checkLimit(identity);
  
  assertThat(result.isAllowed()).isTrue();
  assertThat(result.getRemainingRequests()).isEqualTo(4);
}

@Test
void testRateLimitExceeded() {
  String identity = "ip:192.168.1.1";
  
  // Fazer 5 requisições (limite padrão)
  for (int i = 0; i < 5; i++) {
    rateLimitService.checkLimit(identity);
  }
  
  // 6ª requisição deve ser bloqueada
  RateLimitResult result = rateLimitService.checkLimit(identity);
  assertThat(result.isAllowed()).isFalse();
  assertThat(result.getReason()).isEqualTo("RATE_LIMIT_EXCEEDED");
}
```

---

## 7. Observabilidade e Monitoramento

### 7.1 Logs Estruturados

Requisições bloqueadas são registradas em formato JSON estruturado:

```json
{
  "timestamp": "2026-03-06T10:45:30.123Z",
  "level": "WARN",
  "logger": "com.shieldapi.security.RateLimitingFilter",
  "message": "request_blocked",
  "fields": {
    "reason": "RATE_LIMIT_EXCEEDED",
    "identity": "ip:192.168.1.100",
    "ip": "192.168.1.100",
    "path": "/api/test",
    "method": "GET",
    "status": 429,
    "retry_after": 45
  }
}
```

### 7.2 Métricas Recomendadas

Para integração com Prometheus/Grafana, expor as seguintes métricas:

- `shieldapi_requests_total`: Total de requisições processadas (com labels: endpoint, status)
- `shieldapi_requests_blocked_total`: Total de requisições bloqueadas (com labels: reason)
- `shieldapi_rate_limit_violations`: Violações de rate limit por identidade
- `shieldapi_active_bans`: Número de bans ativos
- `shieldapi_jwt_validations_total`: Total de validações JWT (com labels: valid, invalid)
- `shieldapi_db_connection_pool_size`: Tamanho do pool de conexões PostgreSQL
- `shieldapi_redis_connection_latency_ms`: Latência de conexão com Redis

### 7.3 Alertas Sugeridos

| Alerta | Condição | Severidade |
|---|---|---|
| Taxa Alta de Bloqueios | > 100 requisições bloqueadas/min | Crítica |
| Muitos Bans Ativos | > 50 identidades banidas simultaneamente | Alta |
| Falha de Conectividade Redis | Redis indisponível por > 30s | Crítica |
| Falha de Conectividade BD | PostgreSQL indisponível por > 30s | Crítica |
| Taxa de Erro JWT | > 5% de validações falhando | Média |

---

## 8. Segurança — Boas Práticas

### 8.1 Hardening para Produção

**1. Gerenciamento de Secrets:**
- Nunca commitar `.env` ou `application.yml` com valores reais
- Usar AWS Secrets Manager, HashiCorp Vault, ou Azure Key Vault
- Rotacionar `JWT_SECRET` regularmente
- Usar HTTPS/TLS para todas as comunicações

**2. Autenticação Multifator (MFA):**
- Implementar TOTP (Time-based One-Time Password) via Google Authenticator
- Exigir MFA para contas administrativas

**3. Validação de Entrada:**
- Implementar regex rigoroso para username/email
- Sanitizar todas as entradas para prevenir SQL injection
- Usar prepared statements (já implementado via Spring Data JPA)

**4. Criptografia:**
- Usar bcrypt com salt para hashing de senhas (não MD5 ou SHA1)
- Criptografar dados sensíveis em repouso (PII)
- Usar TLS 1.3+ para dados em trânsito

**5. Auditoria:**
- Registrar todas as operações sensíveis (login, mudança de permissões)
- Implementar Flyway/Liquibase para versionamento de schema
- Manter logs por no mínimo 90 dias

### 8.2 Configuração Segura de CORS

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
      .allowedOrigins("https://trusted-domain.com")
      .allowedMethods("GET", "POST", "PUT", "DELETE")
      .allowedHeaders("Authorization", "Content-Type")
      .allowCredentials(true)
      .maxAge(3600);
  }
}
```

### 8.3 Rate Limiting Adaptativo

Para ambientes de produção, considerar implementar rate limiting adaptativo que ajusta limites baseado em:
- Horário do dia (limites mais altos em horários de pico)
- Reputação do IP (IPs com histórico limpo recebem limites maiores)
- Tipo de endpoint (endpoints críticos têm limites mais restritivos)

---

## 9. Troubleshooting

### 9.1 Problemas Comuns

**Problema: "Redis connection refused"**

**Causa:** Redis não está rodando ou não está acessível.

**Solução:**
```bash
# Verificar se Redis está rodando
redis-cli ping

# Se não estiver, iniciar Redis
redis-server

# Se usar Docker Compose
docker compose up redis -d
```

---

**Problema: "PostgreSQL authentication failed"**

**Causa:** Credenciais incorretas ou usuário não existe.

**Solução:**
```bash
# Conectar ao PostgreSQL
psql -h localhost -U postgres

# Criar usuário se não existir
CREATE USER shield WITH PASSWORD 'shield';
CREATE DATABASE shieldapi OWNER shield;
GRANT ALL PRIVILEGES ON DATABASE shieldapi TO shield;
```

---

**Problema: "JWT token expired"**

**Causa:** Token expirou (padrão: 1 hora).

**Solução:** Fazer novo login para obter token fresco.

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","password":"SecurePass123!"}'
```

---

**Problema: "429 Too Many Requests"**

**Causa:** Limite de requisições excedido.

**Solução:**
- Aguardar tempo indicado no header `Retry-After`
- Verificar se há requisições duplicadas sendo enviadas
- Aumentar `RATE_LIMIT` se necessário (cuidado: pode comprometer segurança)

---

### 9.2 Verificação de Saúde

**Health Check Endpoint (recomendado adicionar):**

```bash
curl http://localhost:8080/actuator/health
```

Resposta esperada:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

---

## 10. Roadmap e Melhorias Futuras

### 10.1 Curto Prazo (1-2 meses)

- [ ] Implementar `AuthenticationEntryPoint` customizado para retornar `401` em JSON
- [ ] Adicionar Flyway para versionamento de schema do banco
- [ ] Expor métricas Prometheus em `/actuator/prometheus`
- [ ] Implementar distributed tracing com Jaeger/Zipkin
- [ ] Adicionar testes de carga com JMeter/Gatling

### 10.2 Médio Prazo (2-4 meses)

- [ ] Suporte a OAuth 2.0 (Google, GitHub, Microsoft)
- [ ] Autenticação Multifator (TOTP)
- [ ] Rate limiting adaptativo baseado em reputação de IP
- [ ] Dashboard de administração para gerenciar usuários e bans
- [ ] Integração com WAF (Web Application Firewall)

### 10.3 Longo Prazo (4+ meses)

- [ ] Suporte a SAML 2.0 para integração enterprise
- [ ] Análise de anomalias com ML (detecção de padrões de ataque)
- [ ] Suporte a API keys com permissões granulares
- [ ] Replicação de Redis para alta disponibilidade
- [ ] Migração para arquitetura de microserviços

---

## 11. Referências e Recursos

| Recurso | URL | Descrição |
|---|---|---|
| Spring Boot Documentation | https://spring.io/projects/spring-boot | Documentação oficial do Spring Boot |
| Spring Security | https://spring.io/projects/spring-security | Framework de segurança para Spring |
| JWT.io | https://jwt.io | Informações sobre JSON Web Tokens |
| Redis Documentation | https://redis.io/documentation | Documentação oficial do Redis |
| PostgreSQL Documentation | https://www.postgresql.org/docs | Documentação oficial do PostgreSQL |
| Docker Documentation | https://docs.docker.com | Guia oficial do Docker |
| OpenAPI 3.0 Specification | https://spec.openapis.org/oas/v3.0.3 | Especificação OpenAPI |
| OWASP Top 10 | https://owasp.org/www-project-top-ten | Top 10 vulnerabilidades web |

---

## 12. Suporte e Contribuição

### 12.1 Reportar Bugs

Para reportar bugs, abra uma issue no repositório com:
- Descrição clara do problema
- Passos para reproduzir
- Versão do Java, Spring Boot e dependências
- Logs relevantes

### 12.2 Contribuir

1. Fork o repositório
2. Crie uma branch para sua feature (`git checkout -b feature/minha-feature`)
3. Commit suas mudanças (`git commit -am 'Adiciona minha feature'`)
4. Push para a branch (`git push origin feature/minha-feature`)
5. Abra um Pull Request

### 12.3 Licença

ShieldAPI é licenciado sob MIT License. Veja `LICENSE` para detalhes.

---

## 📊 Apêndice — Diagramas Técnicos

### A.1 Diagrama de Arquitetura

O diagrama abaixo mostra a arquitetura completa do ShieldAPI com todas as camadas:

- **CLIENT LAYER** (Azul): Clientes HTTP (Web, Mobile, CLI)
- **GATEWAY & SECURITY** (Laranja): Load Balancer, NGINX, WAF
- **SECURITY FILTERS** (Roxo): Filtros de segurança em cadeia
- **REST CONTROLLERS** (Rosa): Endpoints REST
- **BUSINESS LOGIC** (Lilás): Serviços de negócio
- **DATA ACCESS** (Verde): Repositórios JPA
- **PERSISTENCE** (Vermelho): PostgreSQL e Redis
- **OBSERVABILITY** (Verde Claro): Logs, Métricas, Traces

### A.2 Fluxo de Rate Limiting

O fluxo de processamento de uma requisição passa por múltiplas verificações:

1. **Ban Check**: Verifica se identidade está banida
2. **Window Cleanup**: Remove timestamps antigos
3. **Request Count**: Conta requisições na janela
4. **Limit Decision**: Verifica se limite foi excedido
5. **Violation Check**: Incrementa violações se necessário
6. **Ban Activation**: Ativa ban se 3 violações
7. **JWT Validation**: Valida autenticidade do token
8. **Authorization**: Verifica permissões
9. **Processing**: Processa requisição se tudo OK

### A.3 Máquina de Estados

A requisição passa por diversos estados até ser processada ou bloqueada:

- **WAITING**: Aguardando processamento
- **CHECKING_BAN**: Verificando ban
- **CHECKING_LIMIT**: Verificando limite
- **ALLOWED**: Dentro do limite
- **VALIDATING**: Validando JWT
- **AUTHORIZED**: Autorizado
- **SUCCESS**: Processada com sucesso
- **BLOCKED**: Bloqueada (429, 401, 403)

---