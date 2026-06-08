# AGENTS.md — AI Coding Agents Guide for AuthKit-Lite

This file is the operating manual for AI agents, coding assistants, and human reviewers working on this repository.

AuthKit-Lite is a minimal Spring Boot JWT authentication boilerplate. The project is intentionally small, so changes must preserve the clean structure, security posture, and beginner-friendly readability.

## AI Documentation Files

Use these AI-focused files at the repository root:

- `AGENTS.md` — operating manual for AI agents.
- `ARCHITECTURE.md` — current system architecture and request flow.
- `AI_RULES.md` — strict safety, security, and code-generation rules.
- `AGENT_CONTRIBUTING.md` — AI-agent contribution workflow.

Keep the existing `CONTRIBUTING.md` for human contributors. Do not replace it.

---

## 1. Repository Snapshot

- **Application type:** Spring Boot REST API
- **Language:** Java 21
- **Build tool:** Maven
- **Primary package:** `com.auth`
- **Database:** MySQL through Spring Data JPA
- **Security model:** Stateless JWT access tokens + refresh tokens
- **Authorization model:** Role-based access control using Spring Security authorities
- **Current roles:** `ROLE_USER`, `ROLE_ADMIN`
- **JWT library:** Auth0 Java JWT
- **Password hashing:** BCrypt through Spring Security

---

## 2. Read These Before Editing

Before making changes, read these files in this order:

1. `README.md`
2. `ARCHITECTURE.md`
3. `AI_RULES.md`
4. `AGENT_CONTRIBUTING.md`
5. `SECURITY.md`
6. Existing human contributor guide: `CONTRIBUTING.md`
7. `src/main/resources/application.properties`
8. The exact controller/service/security/entity files affected by the task

Do not infer architecture from generic Spring Boot habits. Verify the current implementation first.

Important: this repository already has a human-facing `CONTRIBUTING.md`. Do not overwrite it with AI-agent instructions. AI-specific workflow belongs in `AGENT_CONTRIBUTING.md`.

---

## 3. Important Project Paths

```text
src/main/java/com/auth/
├── AuthKitApplication.java
├── config/
│   ├── DataInitializer.java
│   └── SecurityConfig.java
├── controllers/
│   ├── AuthController.java
│   └── UserController.java
├── dtos/
│   ├── ErrorResponse.java
│   ├── JwtResponse.java
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── UserProfileDto.java
│   └── UserSummaryDto.java
├── entities/
│   ├── RefreshToken.java
│   ├── Role.java
│   └── User.java
├── exceptions/
│   ├── AccessDeniedBusinessException.java
│   ├── EmailAlreadyExistsException.java
│   ├── GlobalExceptionHandler.java
│   ├── InvalidCredentialsException.java
│   ├── RefreshTokenException.java
│   └── UsernameAlreadyExistsException.java
├── repositories/
│   ├── RefreshTokenRepository.java
│   ├── RoleRepository.java
│   └── UserRepository.java
├── security/
│   ├── AuthService.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtUtils.java
│   └── RefreshTokenService.java
└── services/
    └── UserService.java

src/main/resources/
└── application.properties

src/test/java/com/auth/
└── AuthKitApplicationTests.java
```

---

## 4. Local Commands

Use the Maven wrapper if it exists in the local clone. If this repository does not include a wrapper, use system Maven.

```bash
# Run tests
mvn test

# Run the app
mvn spring-boot:run

# Package without tests
mvn -DskipTests package

# Full verification before a PR
mvn clean test
```

If a future contributor adds `mvnw` / `mvnw.cmd`, prefer:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

---

## 5. Coding Boundaries for AI Agents

### Controllers

Controllers should stay thin.

Allowed responsibilities:

- HTTP routing
- Request/response DTO handling
- Calling services
- Returning `ResponseEntity`
- Applying endpoint-level authorization annotations when needed

Avoid:

- Password hashing inside controllers
- JWT generation inside controllers
- Direct repository access inside controllers
- Business rules inside controllers

### Services and Security

Keep auth/security logic in the existing security/service layers:

- `AuthService` handles registration and login workflows.
- `RefreshTokenService` handles refresh-token lifecycle.
- `JwtUtils` handles JWT creation, parsing, expiry, and validation.
- `JwtAuthenticationFilter` reads bearer tokens and populates Spring Security context.
- `UserService` handles user-facing business queries and user DTO conversion.

### Repositories

Repositories must remain persistence-focused. Do not add business logic to repository classes.

### DTOs

Use DTOs for API requests and responses. Do not expose sensitive fields such as passwords, password hashes, refresh-token internals, or security implementation details.

---

## 6. Security Rules

These rules are non-negotiable:

1. Never commit real secrets, production passwords, private keys, or real JWT signing secrets.
2. Never log passwords, password hashes, access tokens, refresh tokens, or full authorization headers.
3. Passwords must be hashed with `PasswordEncoder` / BCrypt before persistence.
4. Keep the API stateless unless a task explicitly requires a different architecture.
5. Do not weaken JWT validation for convenience.
6. Do not return refresh-token values in error messages.
7. Do not expose user password fields through DTOs or entity serialization.
8. New protected endpoints must have clear authorization via Spring Security config and/or `@PreAuthorize`.
9. Keep role authorities consistent with the existing `ROLE_*` naming pattern.
10. Treat seeded demo users as development-only data.

---

## 7. Known Repo-Specific Risks

These are not blockers for normal development, but AI agents should keep them visible:

### 7.1 Demo Credentials

`DataInitializer` creates default roles and demo users. This is useful for local development, but should be guarded behind a development profile or removed before production deployment.

### 7.2 Logout Error Handling

`AuthController.logout` should not return `null` if logout fails. Prefer consistent exception handling through `GlobalExceptionHandler` and a meaningful `ErrorResponse`.

### 7.3 H2 Console Permit Rule

`SecurityConfig` permits `/h2-console/**` and configures frame options for development convenience. The current application properties are MySQL-oriented, so this should be reviewed before production hardening.

### 7.4 Validation

Request DTOs should ideally use Jakarta Bean Validation annotations such as `@NotBlank`, `@Email`, and `@Size`. If validation is added, include the required Spring validation dependency and controller-level `@Valid` usage.

### 7.5 Database Migrations

The current setup uses Hibernate DDL behavior through configuration. For production readiness, prefer Flyway or Liquibase migrations instead of relying on `ddl-auto=update`.

---

## 8. Expected AI Workflow

For every coding task:

1. Identify the exact affected files.
2. Read the existing implementation before editing.
3. Make the smallest safe change.
4. Add or update tests when behavior changes.
5. Run at least `mvn test` when possible.
6. Update documentation if endpoints, configuration, roles, or flows change.
7. Report changed files, commands run, and remaining risks.

---

## 9. Output Format for AI Coding Work

When an AI agent completes a task, respond with:

```text
Summary
- What changed and why.

Changed files
- path/to/File.java — short reason

Validation
- Command run: mvn test
- Result: passed / failed / not run with reason

Risk notes
- Any security, migration, compatibility, or follow-up concerns.
```

Do not bury failed tests or skipped validation. Say it clearly.

---

## 10. Definition of Done

A change is ready for review when:

- The implementation matches the existing architecture.
- Security rules are not weakened.
- Tests pass or any failure is clearly explained.
- DTOs do not leak sensitive fields.
- New endpoints have explicit access rules.
- Documentation is updated when behavior changes.
- The diff is focused and does not include unrelated formatting churn.
