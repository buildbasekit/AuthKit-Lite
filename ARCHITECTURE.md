# ARCHITECTURE.md — AuthKit-Lite Architecture

This document explains how AuthKit-Lite is structured, how requests move through the system, and where future changes should be made.

---

## AI-Assisted Coding Documents

This architecture document is part of the repository's AI-assistance documentation set:

- `AGENTS.md`
- `ARCHITECTURE.md`
- `AI_RULES.md`
- `AGENT_CONTRIBUTING.md`

The existing `CONTRIBUTING.md` remains the human-facing contribution guide.

---

## 1. Product Overview

AuthKit-Lite is a minimal authentication starter for Spring Boot applications. It provides the core backend pieces required for username/email registration, login, JWT-based authentication, refresh tokens, and role-based authorization.

The repository is intentionally compact. Its main goal is to provide a clean baseline that developers can copy, extend, and integrate into larger Spring Boot projects.

---

## 2. Technology Stack

| Area | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.x |
| Build | Maven |
| Web API | Spring Web |
| Security | Spring Security |
| Authentication | JWT access tokens + refresh tokens |
| JWT implementation | Auth0 Java JWT |
| Persistence | Spring Data JPA |
| Database | MySQL |
| Password hashing | BCrypt |
| Boilerplate reduction | Lombok |
| Tests | Spring Boot Test + Spring Security Test |

---

## 3. High-Level Structure

```text
com.auth
├── config          # Spring and startup configuration
├── controllers     # REST API endpoints
├── dtos            # API request/response models
├── entities        # JPA entities
├── exceptions      # Custom exceptions and centralized error handling
├── repositories    # Spring Data JPA repositories
├── security        # Authentication, JWT, filter, refresh-token logic
└── services        # Business-facing service logic
```

The architecture follows a simple layered model:

```text
Client
  ↓
Controller layer
  ↓
Service / Security layer
  ↓
Repository layer
  ↓
Database
```

Spring Security sits across the request pipeline and authenticates protected requests before controller methods execute.

---

## 4. Main Packages

### 4.1 `config`

Contains framework configuration and startup initialization.

Current files:

- `SecurityConfig.java`
- `DataInitializer.java`

Responsibilities:

- Configure stateless Spring Security.
- Register the JWT authentication filter.
- Configure password hashing.
- Enable method-level authorization.
- Seed default roles and demo users for local use.

Production note: demo data initialization should be guarded by a development profile before real deployment.

---

### 4.2 `controllers`

Contains REST controllers.

Current files:

- `AuthController.java`
- `UserController.java`

Responsibilities:

- Expose authentication endpoints.
- Expose authenticated user endpoints.
- Apply endpoint-level access control where needed.
- Return DTOs and `ResponseEntity` responses.

Controllers should not own business logic, password hashing, token generation, or direct database operations.

---

### 4.3 `dtos`

Contains API request and response objects.

Current files:

- `ErrorResponse.java`
- `JwtResponse.java`
- `LoginRequest.java`
- `RegisterRequest.java`
- `UserProfileDto.java`
- `UserSummaryDto.java`

Responsibilities:

- Shape incoming API payloads.
- Shape outgoing API responses.
- Prevent leaking internal entity details.
- Keep API contracts stable even if entities evolve.

Recommended improvement: add Bean Validation annotations to request DTOs and validate them with `@Valid` in controllers.

---

### 4.4 `entities`

Contains JPA entities.

Current files:

- `User.java`
- `Role.java`
- `RefreshToken.java`

Responsibilities:

- Represent persisted users, roles, and refresh tokens.
- Define entity relationships.
- Support authentication and authorization data models.

Entities should not be used as public API responses unless intentionally sanitized.

---

### 4.5 `exceptions`

Contains application-specific exceptions and global error mapping.

Current files include:

- `GlobalExceptionHandler.java`
- `InvalidCredentialsException.java`
- `RefreshTokenException.java`
- `UsernameAlreadyExistsException.java`
- `EmailAlreadyExistsException.java`
- `AccessDeniedBusinessException.java`

Responsibilities:

- Represent business and security failures clearly.
- Convert exceptions into consistent error responses.
- Avoid exposing sensitive implementation details.

---

### 4.6 `repositories`

Contains Spring Data repositories.

Current files:

- `UserRepository.java`
- `RoleRepository.java`
- `RefreshTokenRepository.java`

Responsibilities:

- Query and persist entities.
- Define simple lookup methods.
- Keep database access isolated from controllers.

Repositories should remain persistence-focused and should not contain business workflows.

---

### 4.7 `security`

Contains authentication and token lifecycle logic.

Current files:

- `AuthService.java`
- `JwtAuthenticationFilter.java`
- `JwtUtils.java`
- `RefreshTokenService.java`

Responsibilities:

- Register users.
- Authenticate login requests.
- Create and validate JWT access tokens.
- Create, validate, refresh, and revoke refresh tokens.
- Populate Spring Security context for authenticated requests.

This package is security-critical. Changes here require tests and careful review.

---

### 4.8 `services`

Contains application service logic outside the low-level security package.

Current file:

- `UserService.java`

Responsibilities:

- Fetch user profile data.
- Return user summaries for admin use.
- Apply business-level access decisions where required.

---

## 5. API Surface

### Public Authentication Endpoints

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
```

### Authenticated User Endpoints

```http
GET /api/users/me
GET /api/users/profile
GET /api/users/all
GET /api/users/restricted
GET /api/users/admin-only
```

Access rules are enforced through Spring Security configuration and method-level annotations such as `@PreAuthorize`.

---

## 6. Authentication Flow

### 6.1 Register

```text
Client
  → POST /api/auth/register
  → AuthController
  → AuthService.register(...)
  → Validate username/email uniqueness
  → Hash password with PasswordEncoder
  → Attach default role
  → Save user
  → Return success response
```

### 6.2 Login

```text
Client
  → POST /api/auth/login
  → AuthController
  → AuthService.login(...)
  → Validate credentials
  → Generate JWT access token
  → Create refresh token
  → Return JwtResponse
```

### 6.3 Authenticated Request

```text
Client sends Authorization: Bearer <access-token>
  → JwtAuthenticationFilter
  → JwtUtils validates token
  → Filter loads user identity and authorities
  → SecurityContext is populated
  → Controller method executes if access rules pass
```

### 6.4 Refresh Token

```text
Client
  → POST /api/auth/refresh
  → RefreshTokenService validates refresh token
  → New access token is generated
  → Response returns access token and refresh token data
```

### 6.5 Logout

```text
Client
  → POST /api/auth/logout
  → RefreshTokenService.logout(...)
  → Refresh token is invalidated/deleted
  → Client discards local access token
```

JWT access tokens are stateless. Logout only affects refresh-token reuse unless token denylisting is added later.

---

## 7. Security Model

### 7.1 Stateless Sessions

The app uses stateless session management. The server does not maintain an HTTP session for logged-in users.

### 7.2 Public Routes

`/api/auth/**` is public so users can register, login, refresh, and logout.

### 7.3 Protected Routes

All non-public endpoints require authentication unless explicitly permitted.

### 7.4 Method-Level Authorization

`@EnableMethodSecurity` enables annotations such as:

```java
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
@PreAuthorize("isAuthenticated()")
```

### 7.5 Roles

The current roles are:

- `ROLE_USER`
- `ROLE_ADMIN`

Keep the `ROLE_` prefix unless a deliberate migration is made across the whole system.

---

## 8. Configuration Model

Configuration currently lives in:

```text
src/main/resources/application.properties
```

Important groups:

- Spring application name
- MySQL datasource URL, username, and password
- Hibernate/JPA behavior
- JWT signing secret
- Access-token expiry
- Refresh-token expiry

Recommended production direction:

- Move secrets to environment variables.
- Split config into `application-dev.properties` and `application-prod.properties`.
- Avoid `ddl-auto=update` in production.
- Use Flyway or Liquibase for schema migrations.

---

## 9. Extension Points

### Add a new protected endpoint

1. Add request/response DTOs if needed.
2. Add service method for business logic.
3. Add controller endpoint.
4. Protect it with `@PreAuthorize` or security config.
5. Add tests for authenticated, unauthorized, and forbidden cases.
6. Update API docs/Postman collection if applicable.

### Add a new role

1. Add the role to seed/init logic or migrations.
2. Update access-control annotations where needed.
3. Update tests.
4. Document the new role and its permissions.

### Add new user fields

1. Update `User` entity.
2. Update DTOs only for fields that should be public.
3. Update service mapping.
4. Add database migration if migration tooling exists.
5. Add tests.

### Add production-grade validation

1. Add Spring validation dependency if absent.
2. Add annotations to request DTOs.
3. Use `@Valid` in controller methods.
4. Add validation error handling to `GlobalExceptionHandler`.
5. Add negative tests.

---

## 10. Testing Strategy

AuthKit-Lite should prioritize tests around security behavior.

Recommended test categories:

- Application context load test
- Registration success/failure
- Duplicate username/email
- Login success/failure
- Password hashing verification
- JWT creation and validation
- Expired/invalid token handling
- Refresh-token lifecycle
- Logout behavior
- Access to user endpoint with valid token
- Access denied without token
- Access denied with wrong role
- Admin-only endpoint access

Security changes without tests should be treated as high risk.

---

## 11. Current Non-Goals

The repository currently does not appear to provide:

- OAuth2 social login
- Email verification
- Password reset flow
- Multi-factor authentication
- Frontend UI
- Tenant isolation
- Token denylisting for access-token logout
- Production migration tooling
- Full audit logging

These can be added later, but should be introduced deliberately and documented.
