# ShieldAPI — Arquitetura de Software Profissional

**Versão:** 3.0   
**Data:** Março de 2026  
**Status:** Especificação Arquitetural  
**Objetivo:** Documentação técnica completa seguindo SOLID, Clean Architecture e Design Patterns

---

## 📋 Sumário Executivo

ShieldAPI é uma solução de backend robusta, escalável e segura desenvolvida com **Java 17** e **Spring Boot 3**, especializada em proteção distribuída de endpoints através de autenticação JWT e rate limiting inteligente com Redis. O projeto implementa um modelo de segurança em camadas que combina autenticação, controle de acesso e mitigação de abuso em uma única plataforma coesa, seguindo rigorosamente os princípios **SOLID**, **Clean Architecture** e **Design Patterns** reconhecidos pela indústria.

A arquitetura foi projetada para ambientes de produção, oferecendo resiliência, observabilidade, testabilidade e conformidade com práticas de segurança moderna. O sistema é containerizado via Docker Compose, facilitando deploy em qualquer infraestrutura com garantia de reprodutibilidade.

---

## 📑 Índice

1. [Visão Geral da Solução](#1-visão-geral-da-solução)
2. [Princípios Arquiteturais](#2-princípios-arquiteturais)
3. [Arquitetura em Camadas](#3-arquitetura-em-camadas)
4. [Padrões de Design Implementados](#4-padrões-de-design-implementados)
5. [Estrutura de Diretórios](#5-estrutura-de-diretórios)
6. [Componentes Principais](#6-componentes-principais)
7. [Fluxos de Dados](#7-fluxos-de-dados)
8. [Rate Limiting — Internals](#8-rate-limiting--internals)
9. [Segurança e Autenticação](#9-segurança-e-autenticação)
10. [Endpoints da API](#10-endpoints-da-api)
11. [Configuração e Deployment](#11-configuração-e-deployment)
12. [Testes e Qualidade](#12-testes-e-qualidade)
13. [Observabilidade e Monitoramento](#13-observabilidade-e-monitoramento)
14. [Troubleshooting](#14-troubleshooting)
15. [Roadmap e Evolução](#15-roadmap-e-evolução)

---

## 1. Visão Geral da Solução

### 1.1 Objetivo e Escopo

ShieldAPI resolve o desafio crítico de proteger endpoints de API contra abusos, ataques de força bruta e consumo excessivo de recursos. O sistema implementa três camadas de proteção ortogonais:

**Autenticação:** Validação de identidade via JWT (JSON Web Tokens) com assinatura criptográfica, garantindo que apenas usuários legítimos acessem recursos.

**Autorização:** Controle de acesso baseado em identidade (RBAC — Role-Based Access Control), permitindo permissões granulares por endpoint e recurso.

**Rate Limiting:** Limitação distribuída de requisições com Redis, implementando o algoritmo Sliding Window para detecção precisa de abuso sem falsos positivos.

### 1.2 Stack Tecnológico

| Componente | Tecnologia | Versão | Justificativa Arquitetural |
|---|---|---|---|
| **Runtime** | Java | 17 LTS | Estabilidade, performance, ecosistema maduro, suporte de longo prazo |
| **Framework Web** | Spring Boot | 3.x | Produtividade, convenção sobre configuração, ecossistema robusto |
| **Segurança** | Spring Security | 6.x | Autenticação, autorização, proteção CSRF, integração com filtros |
| **Autenticação** | JWT (jjwt) | 0.12+ | Tokens sem estado, escalável, compatível com microserviços |
| **Banco de Dados** | PostgreSQL | 14+ | ACID, confiabilidade, suporte a JSON, performance com índices |
| **Cache/Rate Limiting** | Redis | 7.x | Sliding window distribuído, performance, operações atômicas |
| **Build** | Maven | 3.9+ | Gerenciamento de dependências, reprodutibilidade, integração CI/CD |
| **Containerização** | Docker | 20.10+ | Isolamento, portabilidade, reprodutibilidade, orquestração |
| **Documentação API** | Swagger/OpenAPI | 3.0 | Especificação padronizada, UI interativa, geração de clientes |

### 1.3 Características Principais

**Autenticação e Autorização:** Registro de usuários com validação de entrada, login com emissão de JWT com expiração configurável, endpoints protegidos com validação de token, estrutura pronta para RBAC com permissões granulares.

**Rate Limiting Distribuído:** Algoritmo Sliding Window implementado via Redis ZSET, identificação por usuário (quando autenticado) ou IP (quando anônimo), limite configurável com padrão de 5 requisições por 60 segundos, ban temporário automático com 3 violações consecutivas resultando em 5 minutos de bloqueio.

**Observabilidade:** Logs estruturados em JSON para requisições bloqueadas, campos contextuais incluindo motivo, identidade, IP, caminho e método, integração com sistemas de logging centralizados como ELK, Splunk e Datadog.

**Documentação Interativa:** Swagger UI integrada em `/swagger-ui.html`, OpenAPI 3.0 em `/v3/api-docs`, endpoints exploráveis e testáveis via UI sem ferramentas externas.

---

## 2. Princípios Arquiteturais

### 2.1 SOLID Principles

**Single Responsibility Principle (SRP):** Cada classe tem uma única razão para mudar. Controllers lidam exclusivamente com HTTP, Services encapsulam lógica de negócio, Repositories abstraem acesso a dados, Filters implementam segurança.

```java
// ✅ BOM - Responsabilidade única
@Service
public class RateLimitService {
    // Apenas lógica de rate limiting
    public RateLimitResult checkLimit(String identity) { /* ... */ }
}

// ❌ RUIM - Múltiplas responsabilidades
@Service
public class UserService {
    // Mistura CRUD, validação, rate limiting, logging, etc.
}
```

**Open/Closed Principle (OCP):** Classes abertas para extensão, fechadas para modificação. Usar interfaces e herança para adicionar funcionalidades sem alterar código existente.

```java
// ✅ BOM - Extensível via interface
public interface RateLimitStrategy {
    RateLimitResult checkLimit(String identity);
}

@Component
public class SlidingWindowStrategy implements RateLimitStrategy {
    public RateLimitResult checkLimit(String identity) { /* ... */ }
}

// Adicionar nova estratégia sem modificar código existente
@Component
public class TokenBucketStrategy implements RateLimitStrategy {
    public RateLimitResult checkLimit(String identity) { /* ... */ }
}
```

**Liskov Substitution Principle (LSP):** Subclasses devem ser substituíveis por suas superclasses sem quebrar o contrato. Implementações de interfaces devem cumprir completamente o contrato definido.

```java
// ✅ BOM - Implementações são substituíveis
List<RateLimitStrategy> strategies = List.of(
    new SlidingWindowStrategy(),
    new TokenBucketStrategy(),
    new FixedWindowStrategy()
);

for (RateLimitStrategy strategy : strategies) {
    RateLimitResult result = strategy.checkLimit("user:123");
    // Funciona com qualquer implementação
}
```

**Interface Segregation Principle (ISP):** Clientes não devem depender de interfaces que não usam. Interfaces pequenas e específicas são preferíveis a interfaces genéricas.

```java
// ✅ BOM - Interfaces segregadas
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}

public interface AuditRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByUserId(UUID userId);
}

// ❌ RUIM - Interface genérica
public interface Repository<T> {
    T save(T entity);
    T findById(UUID id);
    List<T> findAll();
    void delete(T entity);
    // ... 50 outros métodos que nem todos precisam
}
```

**Dependency Inversion Principle (DIP):** Depender de abstrações, não de implementações concretas. Usar injeção de dependência para desacoplamento.

```java
// ✅ BOM - Depende de abstração
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    
    public AuthService(UserRepository userRepository, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }
}

// ❌ RUIM - Depende de implementação concreta
@Service
public class AuthService {
    private UserRepository userRepository = new PostgresUserRepository();
    private JwtTokenProvider tokenProvider = new JwtTokenProviderImpl();
}
```

### 2.2 Clean Architecture

Clean Architecture propõe que o código seja organizado em camadas concêntricas, onde as camadas internas não conhecem as externas, garantindo independência de frameworks, UI, banco de dados e agências externas.

**Independência de Frameworks:** Lógica de negócio não depende de Spring, JPA ou qualquer framework específico. Frameworks são apenas detalhes de implementação.

**Testabilidade:** Componentes podem ser testados sem banco de dados, HTTP ou UI. Usar mocks e stubs para isolar unidades de código.

**Independência de UI:** Lógica de negócio não sabe se está sendo chamada por REST, GraphQL, gRPC ou CLI.

**Independência de Banco de Dados:** Trocar PostgreSQL por MySQL ou MongoDB não requer mudanças em lógica de negócio.

**Independência de Agências Externas:** Integração com Redis, email, SMS são detalhes de implementação, não parte do core de negócio.

### 2.3 Domain-Driven Design (DDD)

**Ubiquitous Language:** Termos de negócio (User, RateLimitViolation, SecurityContext) usados consistentemente no código, documentação e comunicação com stakeholders.

**Bounded Contexts:** Domínios separados (Authentication, RateLimiting, Audit) com modelos próprios, evitando acoplamento entre contextos.

**Entities e Value Objects:** Entidades com identidade única (User com UUID) vs Value Objects sem identidade (RateLimitResult, JwtClaims).

**Aggregates:** Agrupamentos de entidades relacionadas com raiz de agregado (User como raiz com Credentials como parte do agregado).

---

## 3. Arquitetura em Camadas

### 3.1 Estrutura de Camadas

A arquitetura segue um padrão em camadas com separação clara de responsabilidades:

**Presentation Layer (Controllers):** Receber requisições HTTP, validar entrada, chamar services, formatar respostas. Não contém lógica de negócio.

**Application Layer (Services):** Orquestrar lógica de negócio, transações, validações complexas. Independente de HTTP.

**Domain Layer (Entities, Value Objects):** Modelos de domínio puros, sem dependências de frameworks.

**Infrastructure Layer (Repositories, Filters, Providers):** Implementações concretas de acesso a dados, segurança, cache.

**Cross-Cutting Concerns (Filters, AOP, Logging):** Aspectos que atravessam múltiplas camadas.

### 3.2 Fluxo de Requisição

```
HTTP Request
    ↓
SecurityFilterChain
    ├─ RateLimitingFilter (Verificar limite)
    ├─ JwtAuthenticationFilter (Validar token)
    └─ AuthorizationFilter (Verificar permissões)
    ↓
REST Controller (Receber requisição)
    ↓
Service Layer (Lógica de negócio)
    ├─ Validações
    ├─ Cálculos
    └─ Orquestração
    ↓
Repository Layer (Acesso a dados)
    ├─ PostgreSQL (Persistência)
    └─ Redis (Cache/Rate Limiting)
    ↓
Response Processing
    ├─ Serialização JSON
    └─ Headers HTTP
    ↓
HTTP Response
```

---

## 4. Padrões de Design Implementados

### 4.1 Filter Pattern (Security Filters)

Implementar cadeia de filtros para processar requisições sequencialmente:

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        String identity = extractIdentity(request);
        RateLimitResult result = rateLimitService.checkLimit(identity);
        
        if (!result.isAllowed()) {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.addHeader("Retry-After", String.valueOf(result.getRetryAfter()));
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 4.2 Strategy Pattern (Rate Limiting Strategies)

Diferentes estratégias de rate limiting podem ser implementadas e trocadas em tempo de execução:

```java
public interface RateLimitStrategy {
    RateLimitResult checkLimit(String identity);
}

@Component
public class SlidingWindowStrategy implements RateLimitStrategy {
    public RateLimitResult checkLimit(String identity) {
        // Implementação Sliding Window
    }
}

@Component
public class TokenBucketStrategy implements RateLimitStrategy {
    public RateLimitResult checkLimit(String identity) {
        // Implementação Token Bucket
    }
}
```

### 4.3 Repository Pattern (Data Access)

Abstração de acesso a dados com Spring Data JPA:

```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.active = true")
    Optional<User> findActiveByEmail(@Param("email") String email);
}
```

### 4.4 Service Layer Pattern

Lógica de negócio centralizada em services com transações:

```java
@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new AuthenticationException("Usuário não encontrado"));
        
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Senha inválida");
        }
        
        String token = tokenProvider.generateToken(user);
        return new AuthResponse(token, user.getId());
    }
}
```

### 4.5 DTO Pattern (Data Transfer Objects)

Separação entre modelo de domínio e transferência de dados:

```java
// Domain Model
@Entity
public class User {
    private UUID id;
    private String email;
    private String passwordHash;
    private Set<Role> roles;
}

// Request DTO
public record LoginRequest(
    @Email String email,
    @NotBlank String password
) {}

// Response DTO
public record AuthResponse(String token, UUID userId) {}
```

### 4.6 Dependency Injection Pattern

Injeção de dependências via Spring:

```java
@Service
public class RateLimitService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimitStrategy strategy;
    
    // Injeção via construtor (preferido)
    public RateLimitService(RedisTemplate<String, Object> redisTemplate, RateLimitStrategy strategy) {
        this.redisTemplate = redisTemplate;
        this.strategy = strategy;
    }
}
```

### 4.7 Observer Pattern (Application Events)

Publicar eventos de domínio para desacoplamento:

```java
@Service
public class AuthService {
    private final ApplicationEventPublisher eventPublisher;
    
    public void login(User user) {
        // Lógica de login
        eventPublisher.publishEvent(new UserLoggedInEvent(user));
    }
}

@Component
public class AuditListener {
    @EventListener
    public void onUserLoggedIn(UserLoggedInEvent event) {
        // Registrar em auditoria sem acoplar AuthService
    }
}
```

### 4.8 Factory Pattern (Object Creation)

Centralizar criação de objetos complexos:

```java
@Component
public class JwtTokenFactory {
    public String createToken(User user) {
        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
            .setIssuedAt(new Date())
            .setExpiration(calculateExpiration())
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
}
```

---

## 5. Estrutura de Diretórios

```
shieldapi/
├── src/main/java/com/shieldapi/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── JwtConfig.java
│   │   ├── RedisConfig.java
│   │   └── WebConfig.java
│   │
│   ├── domain/
│   │   ├── user/
│   │   │   ├── User.java (Entity)
│   │   │   ├── Role.java (Enum)
│   │   │   ├── UserRepository.java (Interface)
│   │   │   └── UserSpecification.java (Query DSL)
│   │   │
│   │   ├── ratelimit/
│   │   │   ├── RateLimitViolation.java (Entity)
│   │   │   ├── RateLimitResult.java (Value Object)
│   │   │   └── RateLimitRepository.java (Interface)
│   │   │
│   │   └── audit/
│   │       ├── AuditLog.java (Entity)
│   │       └── AuditRepository.java (Interface)
│   │
│   ├── application/
│   │   ├── auth/
│   │   │   ├── AuthService.java
│   │   │   ├── LoginRequest.java (DTO)
│   │   │   ├── AuthResponse.java (DTO)
│   │   │   └── AuthServiceImpl.java
│   │   │
│   │   ├── user/
│   │   │   ├── UserService.java
│   │   │   ├── UserRequest.java (DTO)
│   │   │   ├── UserResponse.java (DTO)
│   │   │   └── UserServiceImpl.java
│   │   │
│   │   └── ratelimit/
│   │       ├── RateLimitService.java
│   │       ├── RateLimitServiceImpl.java
│   │       └── RateLimitStrategy.java (Interface)
│   │
│   ├── presentation/
│   │   ├── auth/
│   │   │   ├── AuthController.java
│   │   │   └── AuthExceptionHandler.java
│   │   │
│   │   ├── api/
│   │   │   ├── ApiController.java
│   │   │   └── ApiExceptionHandler.java
│   │   │
│   │   └── health/
│   │       └── HealthController.java
│   │
│   ├── infrastructure/
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── AuthorizationFilter.java
│   │   │   ├── CustomUserDetailsService.java
│   │   │   └── SecurityContextExtractor.java
│   │   │
│   │   ├── persistence/
│   │   │   ├── UserRepositoryImpl.java
│   │   │   ├── RateLimitRepositoryImpl.java
│   │   │   └── AuditRepositoryImpl.java
│   │   │
│   │   ├── ratelimit/
│   │   │   ├── SlidingWindowStrategy.java
│   │   │   ├── TokenBucketStrategy.java
│   │   │   └── RedisRateLimitStore.java
│   │   │
│   │   ├── cache/
│   │   │   └── CacheService.java
│   │   │
│   │   └── audit/
│   │       └── AuditService.java
│   │
│   ├── shared/
│   │   ├── exception/
│   │   │   ├── ShieldApiException.java
│   │   │   ├── AuthenticationException.java
│   │   │   ├── RateLimitException.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   ├── util/
│   │   │   ├── JwtUtils.java
│   │   │   ├── SecurityUtils.java
│   │   │   ├── ValidationUtils.java
│   │   │   └── DateUtils.java
│   │   │
│   │   ├── dto/
│   │   │   ├── ApiResponse.java
│   │   │   ├── ErrorResponse.java
│   │   │   └── PageResponse.java
│   │   │
│   │   └── event/
│   │       ├── DomainEvent.java
│   │       ├── UserLoggedInEvent.java
│   │       └── RateLimitViolationEvent.java
│   │
│   └── ShieldApiApplication.java
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/
│       ├── V1__Initial_Schema.sql
│       ├── V2__Add_Audit_Tables.sql
│       └── V3__Add_Indexes.sql
│
├── src/test/java/com/shieldapi/
│   ├── application/
│   │   ├── auth/
│   │   │   └── AuthServiceTest.java
│   │   └── ratelimit/
│   │       └── RateLimitServiceTest.java
│   │
│   ├── infrastructure/
│   │   ├── security/
│   │   │   └── JwtTokenProviderTest.java
│   │   └── ratelimit/
│   │       └── SlidingWindowStrategyTest.java
│   │
│   └── presentation/
│       ├── auth/
│       │   └── AuthControllerIntegrationTest.java
│       └── api/
│           └── ApiControllerIntegrationTest.java
│
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## 6. Componentes Principais

### 6.1 JWT Token Provider

Geração e validação de tokens JWT com claims customizados:

```java
@Component
public class JwtTokenProvider {
    private final String jwtSecret;
    private final long jwtExpiration;
    
    public String generateToken(User user) {
        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("roles", user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList()))
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public String getUserIdFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
}
```

### 6.2 Rate Limiting Service

Implementação do algoritmo Sliding Window com Redis:

```java
@Service
public class RateLimitService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimitStrategy strategy;
    private final int limitPerWindow;
    private final long windowDurationSeconds;
    
    public RateLimitResult checkLimit(String identity) {
        // Verificar ban
        if (isBanned(identity)) {
            return RateLimitResult.banned(getRemainingBanTime(identity));
        }
        
        // Verificar limite
        RateLimitResult result = strategy.checkLimit(identity);
        
        if (!result.isAllowed()) {
            incrementViolations(identity);
            
            // Ban se 3 violações
            if (getViolationCount(identity) >= 3) {
                activateBan(identity);
                return RateLimitResult.banned(300); // 5 minutos
            }
        }
        
        return result;
    }
    
    private void activateBan(String identity) {
        String banKey = "ban:" + identity;
        redisTemplate.opsForValue().set(banKey, true, Duration.ofSeconds(300));
    }
    
    private boolean isBanned(String identity) {
        String banKey = "ban:" + identity;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().get(banKey));
    }
}
```

### 6.3 Authentication Service

Orquestração de autenticação com validações:

```java
@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ApplicationEventPublisher eventPublisher;
    
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new AuthenticationException("Usuário não encontrado"));
        
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Senha inválida");
        }
        
        String token = tokenProvider.generateToken(user);
        eventPublisher.publishEvent(new UserLoggedInEvent(user));
        
        return new AuthResponse(token, user.getId());
    }
    
    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ValidationException("Email já registrado");
        }
        
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(Role.USER));
        
        userRepository.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(user));
    }
}
```

### 6.4 Global Exception Handler

Tratamento centralizado de exceções:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("Autenticação Falhou", ex.getMessage()));
    }
    
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitException(RateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new ErrorResponse("Limite de Requisições Excedido", ex.getMessage()));
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("Validação Falhou", ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Erro não tratado", ex);
        return ResponseEntity.internalServerError()
            .body(new ErrorResponse("Erro Interno", "Algo deu errado"));
    }
}
```

---

## 7. Fluxos de Dados

### 7.1 Fluxo de Autenticação

```
1. Frontend envia POST /auth/login com email e senha
   ↓
2. AuthController recebe requisição
   ↓
3. AuthService.login() executa:
   ├─ Buscar usuário por email em PostgreSQL
   ├─ Comparar senha com hash usando BCrypt
   ├─ Gerar JWT token com claims
   └─ Publicar evento UserLoggedInEvent
   ↓
4. AuditListener recebe evento e registra login em auditoria
   ↓
5. AuthController retorna AuthResponse com token
   ↓
6. Frontend armazena token em localStorage
   ↓
7. Requisições subsequentes incluem token no header Authorization
```

### 7.2 Fluxo de Rate Limiting

```
1. Requisição HTTP chega
   ↓
2. RateLimitingFilter intercepta
   ↓
3. RateLimitService.checkLimit(identity) executa:
   ├─ Verificar se identidade está banida em Redis
   ├─ Se banido: retornar 429 com Retry-After
   ├─ Se não banido: executar strategy (Sliding Window)
   │  ├─ Remover timestamps antigos (fora da janela)
   │  ├─ Contar requisições na janela
   │  ├─ Se <= limite: adicionar novo timestamp
   │  └─ Se > limite: incrementar violações
   ├─ Se 3 violações: ativar ban por 5 minutos
   └─ Retornar RateLimitResult
   ↓
4. Se bloqueado: retornar 429 e parar
   ↓
5. Se permitido: continuar na cadeia de filtros
```

### 7.3 Fluxo de Autorização

```
1. JwtAuthenticationFilter extrai token do header
   ↓
2. JwtTokenProvider.validateToken() valida assinatura
   ↓
3. Se válido: carregar contexto de segurança com claims
   ↓
4. AuthorizationFilter verifica permissões
   ├─ Obter roles do usuário
   ├─ Verificar se endpoint requer autenticação
   ├─ Verificar se usuário tem role necessária
   └─ Se OK: permitir; se não: retornar 403
   ↓
5. Se autorizado: rotear para controller
```

---

## 8. Rate Limiting — Internals

### 8.1 Algoritmo Sliding Window

O Sliding Window é uma técnica de rate limiting que mantém um registro de timestamps de requisições em uma janela de tempo deslizante:

**Funcionamento:**

1. Para cada identidade, manter um ZSET em Redis com timestamps de requisições
2. Quando nova requisição chega, remover todos os timestamps fora da janela (agora - windowSize)
3. Contar requisições restantes na janela
4. Se count < limit: adicionar novo timestamp e permitir
5. Se count >= limit: bloquear e incrementar violações

**Vantagens:** Precisão, sem falsos positivos, distribuído via Redis, operações atômicas.

### 8.2 Implementação em Redis

```java
public class SlidingWindowStrategy implements RateLimitStrategy {
    private static final String WINDOW_KEY_PREFIX = "rate_limit:window:";
    private static final String VIOLATIONS_KEY_PREFIX = "rate_limit:violations:";
    
    public RateLimitResult checkLimit(String identity) {
        String windowKey = WINDOW_KEY_PREFIX + identity;
        String violationsKey = VIOLATIONS_KEY_PREFIX + identity;
        
        long now = System.currentTimeMillis();
        long windowStart = now - (windowDurationSeconds * 1000);
        
        // Remover timestamps antigos
        redisTemplate.opsForZSet().removeRangeByScore(windowKey, 0, windowStart);
        
        // Contar requisições na janela
        Long count = redisTemplate.opsForZSet().size(windowKey);
        
        if (count >= limitPerWindow) {
            // Incrementar violações
            Long violations = redisTemplate.opsForValue().increment(violationsKey);
            redisTemplate.expire(violationsKey, Duration.ofSeconds(windowDurationSeconds));
            
            return RateLimitResult.blocked(
                "RATE_LIMIT_EXCEEDED",
                (int) (windowDurationSeconds - (now - windowStart) / 1000)
            );
        }
        
        // Adicionar novo timestamp
        redisTemplate.opsForZSet().add(windowKey, UUID.randomUUID().toString(), now);
        redisTemplate.expire(windowKey, Duration.ofSeconds(windowDurationSeconds));
        
        return RateLimitResult.allowed(limitPerWindow - count - 1);
    }
}
```

---

## 9. Segurança e Autenticação

### 9.1 Autenticação JWT

**Estrutura do Token:** Header.Payload.Signature

**Header:** Algoritmo (HS512) e tipo (JWT)

**Payload:** Claims customizados (sub, email, roles, iat, exp)

**Signature:** HMAC-SHA512(header.payload, secret)

### 9.2 Proteção contra Brute Force

```java
@Component
public class LoginAttemptService {
    public void recordFailedLogin(String email) {
        String key = "login:attempts:" + email;
        Long attempts = redisTemplate.opsForValue().increment(key);
        
        if (attempts == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(15));
        }
        
        if (attempts > 5) {
            throw new AccountLockedException("Conta bloqueada. Tente novamente em 15 minutos.");
        }
    }
    
    public void recordSuccessfulLogin(String email) {
        redisTemplate.delete("login:attempts:" + email);
    }
}
```

### 9.3 Validação de Entrada

```java
public record LoginRequest(
    @Email(message = "Email inválido")
    String email,
    
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 128, message = "Senha deve ter entre 8 e 128 caracteres")
    String password
) {}
```

---

## 10. Endpoints da API

| Método | Caminho | Autenticação | Rate Limited | Descrição |
|---|---|---|---|---|
| **POST** | `/auth/register` | Não | Sim | Registrar novo usuário |
| **POST** | `/auth/login` | Não | Sim | Autenticar usuário |
| **GET** | `/api/test` | Sim (JWT) | Sim | Endpoint de teste protegido |
| **GET** | `/actuator/health` | Não | Não | Health check |
| **GET** | `/swagger-ui.html` | Não | Não | Documentação Swagger |

---

## 11. Configuração e Deployment

### 11.1 Variáveis de Ambiente

```dotenv
# JWT
JWT_SECRET=seu-segredo-jwt-com-32-caracteres-minimo
JWT_EXPIRATION_MINUTES=1440

# PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/shieldapi
SPRING_DATASOURCE_USERNAME=shield
SPRING_DATASOURCE_PASSWORD=senha-forte

# Redis
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=senha-redis-forte

# Rate Limiting
RATE_LIMIT_PER_WINDOW=5
RATE_LIMIT_WINDOW_SECONDS=60
```

### 11.2 Docker Compose

```yaml
version: '3.8'

services:
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/shieldapi
      - SPRING_REDIS_HOST=redis
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: shieldapi
      POSTGRES_USER: shield
      POSTGRES_PASSWORD: shield
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U shield"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

---

## 12. Testes e Qualidade

### 12.1 Testes Unitários

```java
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @InjectMocks
    private RateLimitService rateLimitService;
    
    @Test
    void testCheckLimit_AllowedRequest() {
        // Arrange
        String identity = "user:123";
        
        // Act
        RateLimitResult result = rateLimitService.checkLimit(identity);
        
        // Assert
        assertThat(result.isAllowed()).isTrue();
    }
    
    @Test
    void testCheckLimit_ExceededLimit() {
        // Arrange
        String identity = "user:123";
        for (int i = 0; i < 5; i++) {
            rateLimitService.checkLimit(identity);
        }
        
        // Act
        RateLimitResult result = rateLimitService.checkLimit(identity);
        
        // Assert
        assertThat(result.isAllowed()).isFalse();
    }
}
```

### 12.2 Testes de Integração

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testLogin_Success() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        
        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }
}
```

---

## 13. Observabilidade e Monitoramento

### 13.1 Logs Estruturados

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

### 13.2 Métricas Prometheus

```
shieldapi_requests_total{endpoint="/api/test",status="200"} 1523
shieldapi_requests_blocked_total{reason="RATE_LIMIT_EXCEEDED"} 45
shieldapi_rate_limit_violations{identity="ip:192.168.1.100"} 2
shieldapi_active_bans 3
```

---

## 14. Troubleshooting

### 14.1 Problemas Comuns

**Problema: Redis connection refused**

**Solução:** Verificar se Redis está rodando com `redis-cli ping`

**Problema: JWT token expired**

**Solução:** Fazer novo login para obter token fresco

**Problema: 429 Too Many Requests**

**Solução:** Aguardar tempo indicado no header `Retry-After`

---

## 15. Roadmap e Evolução

### 15.1 Curto Prazo (1-2 meses)

- Implementar OAuth 2.0
- Adicionar Flyway para migrations
- Expor métricas Prometheus
- Testes de carga com Gatling

### 15.2 Médio Prazo (2-4 meses)

- Autenticação Multifator (TOTP)
- Rate limiting adaptativo
- Dashboard de administração
- Integração com WAF

### 15.3 Longo Prazo (4+ meses)

- Suporte a SAML 2.0
- Análise de anomalias com ML
- API keys com permissões granulares
- Alta disponibilidade com Redis Cluster

---

**Documento preparado por:** Manus AI  
**Última atualização:** Março de 2026  
**Versão:** 3.0  
**Status:** Especificação Arquitetural Completa
