# 🛡️ ShieldAPI — Proteção Distribuída de Endpoints

[![Java](https://img.shields.io/badge/Java-17-orange?logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green?logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-red?logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-20.10+-blue?logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

**Backend robusto, escalável e seguro para proteção de APIs com autenticação JWT e rate limiting distribuído via Redis.**

ShieldAPI implementa um modelo de segurança em camadas que combina **autenticação**, **controle de acesso** e **mitigação de abuso** em uma única plataforma coesa, seguindo rigorosamente os princípios **SOLID**, **Clean Architecture** e **Design Patterns** reconhecidos pela indústria.

---

## 📋 Sumário

- [Características Principais](#-características-principais)
- [Stack Tecnológico](#-stack-tecnológico)
- [Início Rápido](#-início-rápido)
- [Arquitetura](#-arquitetura)
- [Endpoints da API](#-endpoints-da-api)
- [Segurança](#-segurança)
- [Rate Limiting](#-rate-limiting)
- [Configuração](#-configuração)
- [Desenvolvimento](#-desenvolvimento)
- [Testes](#-testes)
- [Deployment](#-deployment)
- [Troubleshooting](#-troubleshooting)
- [Contribuição](#-contribuição)

---

## ✨ Características Principais

### 🔐 Autenticação e Autorização

- ✅ **Registro de Usuários** — Validação de entrada com BCrypt
- ✅ **Login com JWT** — Tokens com expiração configurável
- ✅ **Endpoints Protegidos** — Validação de token Bearer
- ✅ **RBAC** — Role-Based Access Control com permissões granulares
- ✅ **Logout** — Invalidação de tokens com blacklist

### ⚡ Rate Limiting Distribuído

- ✅ **Algoritmo Sliding Window** — Implementado via Redis ZSET
- ✅ **Identificação Flexível** — Por usuário (autenticado) ou IP (anônimo)
- ✅ **Limite Configurável** — Padrão: 5 requisições por 60 segundos
- ✅ **Ban Automático** — 3 violações = 5 minutos de bloqueio
- ✅ **Retry-After Header** — Informar cliente quando tentar novamente

### 📊 Observabilidade

- ✅ **Logs Estruturados** — JSON com contexto completo
- ✅ **Auditoria** — Rastreamento de ações de usuários
- ✅ **Métricas Prometheus** — Requisições, latência, bloqueios
- ✅ **Distributed Tracing** — Correlação de requisições entre serviços
- ✅ **Health Checks** — Endpoints de status da aplicação

### 📚 Documentação Interativa

- ✅ **Swagger UI** — Exploração visual de endpoints em `/swagger-ui.html`
- ✅ **OpenAPI 3.0** — Especificação em `/v3/api-docs`
- ✅ **Testes Diretos** — Execute requisições via UI sem ferramentas externas

---

## 🛠️ Stack Tecnológico

| Componente | Tecnologia | Versão | Propósito |
|-----------|-----------|--------|----------|
| **Runtime** | Java | 17 LTS | Linguagem, estabilidade, performance |
| **Framework** | Spring Boot | 3.3.5 | Web, segurança, data access |
| **Segurança** | Spring Security | 6.x | Autenticação, autorização, filtros |
| **Autenticação** | JWT (jjwt) | 0.12+ | Tokens sem estado, escalável |
| **Banco de Dados** | PostgreSQL | 14+ | Persistência, ACID, confiabilidade |
| **Cache/Rate Limit** | Redis | 7.x | Sliding window distribuído |
| **Build** | Maven | 3.9+ | Gerenciamento de dependências |
| **Containerização** | Docker | 20.10+ | Isolamento, portabilidade |
| **Documentação** | Swagger/OpenAPI | 3.0 | Especificação, UI interativa |

---

## 🚀 Início Rápido

### Pré-requisitos

- Docker e Docker Compose instalados
- Git para clonar o repositório
- (Opcional) Java 17 e Maven para desenvolvimento local

### Opção 1: Docker Compose (Recomendado)

```bash
# 1. Clonar repositório
git clone https://github.com/seu-usuario/shieldapi.git
cd shieldapi

# 2. Configurar variáveis de ambiente
cp .env.example .env

# 3. Editar .env com valores sensíveis
# DB_PASSWORD, RABBITMQ_PASSWORD, JWT_SECRET, etc.

# 4. Subir ambiente completo
docker compose up --build

# 5. Acessar serviços
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
# RabbitMQ: http://localhost:15672
# PostgreSQL: localhost:5432
```

### Opção 2: Execução Local

```bash
# 1. Subir apenas dependências
docker compose up postgres redis -d

# 2. Executar aplicação com Maven
mvn spring-boot:run

# 3. Acessar API
# http://localhost:8080/swagger-ui.html
```

---

## 🏗️ Arquitetura

### Estrutura em Camadas

```
┌─────────────────────────────────────────┐
│  Presentation Layer (Controllers)       │
│  ├─ AuthController                      │
│  ├─ ApiController                       │
│  └─ HealthController                    │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│  Application Layer (Services)           │
│  ├─ AuthService                         │
│  ├─ UserService                         │
│  └─ RateLimitService                    │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│  Domain Layer (Entities, Value Objects) │
│  ├─ User                                │
│  ├─ RateLimitViolation                  │
│  └─ AuditLog                            │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│  Infrastructure Layer                   │
│  ├─ Repositories (JPA)                  │
│  ├─ Security Filters                    │
│  ├─ Redis Cache                         │
│  └─ JWT Provider                        │
└─────────────────────────────────────────┘
```

### Fluxo de Requisição

```
HTTP Request
    ↓
RateLimitingFilter (Redis Sliding Window)
    ↓
JwtAuthenticationFilter (Validar token)
    ↓
AuthorizationFilter (Verificar permissões)
    ↓
REST Controller
    ↓
Service Layer (Lógica de negócio)
    ↓
Repository Layer (PostgreSQL)
    ↓
HTTP Response
```

---

## 📡 Endpoints da API

### Autenticação

#### Gerar Token JWT

```bash
POST /api/v1/auth/token
Content-Type: application/json

{
  "username": "api-user",
  "password": "sua-senha"
}
```

**Resposta (200 OK):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Registrar Novo Usuário

```bash
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "usuario@example.com",
  "password": "senha-segura",
  "name": "Nome do Usuário"
}
```

### API Protegida

#### Obter Informações do Usuário

```bash
GET /api/v1/users/me
Authorization: Bearer <TOKEN>
```

**Resposta (200 OK):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "usuario@example.com",
  "name": "Nome do Usuário",
  "roles": ["USER"],
  "createdAt": "2026-03-06T10:30:00Z"
}
```

#### Chamar Endpoint Protegido

```bash
GET /api/v1/protected-resource
Authorization: Bearer <TOKEN>
```

### Health Check

```bash
GET /health
```

**Resposta (200 OK):**

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

---

## 🔐 Segurança

### Autenticação JWT

- **Algoritmo:** HS512 (HMAC com SHA-512)
- **Expiração:** Configurável (padrão: 1 hora)
- **Claims:** `sub` (user ID), `email`, `roles`, `iat`, `exp`
- **Refresh:** Implementar endpoint de refresh token para renovação

### Proteção CORS

```yaml
spring:
  web:
    cors:
      allowed-origins: "http://localhost:3000,https://seu-dominio.com"
      allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
      allowed-headers: "Content-Type,Authorization"
      allow-credentials: true
```

### Rate Limiting

- **Limite Padrão:** 5 requisições por 60 segundos
- **Identificação:** Por usuário (autenticado) ou IP (anônimo)
- **Ban Automático:** 3 violações = 5 minutos de bloqueio
- **Header Retry-After:** Informa cliente quando tentar novamente

### Validação de Entrada

```java
@PostMapping("/register")
public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
    // @Email, @NotBlank, @Size validam automaticamente
}
```

---

## ⚡ Rate Limiting

### Algoritmo Sliding Window

O rate limiting usa o algoritmo **Sliding Window** implementado via Redis ZSET:

```
Janela de 60 segundos (timestamp atual - 60000ms)

Requisições:
├─ 10:00:00 ✓
├─ 10:00:05 ✓
├─ 10:00:10 ✓
├─ 10:00:15 ✓
├─ 10:00:20 ✓
└─ 10:00:25 ✗ (6ª requisição = bloqueado)

Retry-After: 45 segundos
```

### Configuração

```yaml
rate-limit:
  enabled: true
  requests-per-window: 5
  window-duration-seconds: 60
  max-violations: 3
  ban-duration-minutes: 5
```

### Teste de Rate Limiting

```bash
# Fazer 6 requisições rápidas
for i in {1..6}; do
  curl -X GET http://localhost:8080/api/v1/protected \
    -H "Authorization: Bearer <TOKEN>"
done

# 6ª requisição retorna 429 Too Many Requests
# Retry-After: 45
```

---

## ⚙️ Configuração

### Variáveis de Ambiente

```bash
# Banco de Dados
DB_HOST=postgres
DB_PORT=5432
DB_NAME=shieldapi
DB_USERNAME=postgres
DB_PASSWORD=sua-senha-segura

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
JWT_SECRET=sua-chave-secreta-muito-segura-aqui-com-minimo-32-caracteres
JWT_EXPIRATION_HOURS=1

# Rate Limiting
RATE_LIMIT_REQUESTS=5
RATE_LIMIT_WINDOW_MINUTES=1

# Segurança
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://seu-dominio.com
```

### application.yml

```yaml
spring:
  application:
    name: shieldapi
  
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION_HOURS}

rate-limit:
  requests: ${RATE_LIMIT_REQUESTS}
  window-minutes: ${RATE_LIMIT_WINDOW_MINUTES}
```

---

## 👨‍💻 Desenvolvimento

### Estrutura de Diretórios

```
src/main/java/com/shieldapi/
├── config/              # Configurações Spring
├── domain/              # Entidades e Value Objects
├── application/         # Services e lógica de negócio
├── presentation/        # Controllers e exception handlers
├── infrastructure/      # Repositories, filters, providers
└── ShieldApiApplication.java
```

### Criar Novo Endpoint

1. **Criar DTO:**

```java
public record UserRequest(
    @Email String email,
    @NotBlank String name
) {}
```

2. **Criar Service:**

```java
@Service
public class UserService {
    public UserResponse createUser(UserRequest request) {
        // Lógica de negócio
    }
}
```

3. **Criar Controller:**

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }
}
```

### Executar Localmente

```bash
# Instalar dependências
mvn clean install

# Executar testes
mvn test

# Executar aplicação
mvn spring-boot:run

# Build JAR
mvn package
```

---

## 🧪 Testes

### Testes Unitários

```bash
mvn test
```

### Testes de Integração

```bash
mvn verify
```

### Cobertura de Testes

```bash
mvn jacoco:report
# Relatório em: target/site/jacoco/index.html
```

### Exemplo de Teste

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldGenerateTokenWithValidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "username": "api-user",
                  "password": "password"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }
}
```

---

## 🚢 Deployment

### Docker

```bash
# Build imagem
docker build -t shieldapi:latest .

# Executar container
docker run -p 8080:8080 \
  -e DB_HOST=postgres \
  -e DB_PASSWORD=senha \
  -e JWT_SECRET=chave-secreta \
  shieldapi:latest
```

### Docker Compose (Produção)

```bash
# Subir ambiente completo
docker compose -f docker-compose.yml up -d

# Ver logs
docker compose logs -f api

# Parar ambiente
docker compose down
```

### Kubernetes

```bash
# Aplicar manifests
kubectl apply -f k8s/

# Verificar deployment
kubectl get pods -l app=shieldapi

# Ver logs
kubectl logs -f deployment/shieldapi
```

---

## 🔧 Troubleshooting

### Erro: "Connection refused" ao PostgreSQL

```bash
# Verificar se PostgreSQL está rodando
docker ps | grep postgres

# Reiniciar serviço
docker compose restart postgres
```

### Erro: "Redis connection failed"

```bash
# Verificar se Redis está rodando
docker ps | grep redis

# Testar conexão
redis-cli -h localhost ping
```

### Erro: "JWT token invalid"

```bash
# Verificar se JWT_SECRET está configurado
echo $JWT_SECRET

# Regenerar token
curl -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"api-user","password":"password"}'
```

### Rate Limit não funciona

```bash
# Verificar configuração
curl http://localhost:8080/actuator/configprops | grep rate-limit

# Resetar contador em Redis
redis-cli DEL "rate-limit:*"
```

---

## 📚 Documentação Adicional

- [Documentação Completa](docs/ARQUITETURA.md) — Detalhes arquiteturais profundos
- [Guia de API](docs/API.md) — Referência completa de endpoints
- [Guia de Operação](docs/OPERACAO.md) — Monitoramento e troubleshooting

