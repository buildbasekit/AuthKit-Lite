# AI_RULES.md — Rules for AI-Assisted Coding in AuthKit-Lite

This file defines strict rules for AI tools working on AuthKit-Lite.

The goal is not just to generate code. The goal is to preserve a secure, clean, production-upgradable authentication starter.

---

## 1. Core Principle

AuthKit-Lite is an authentication project. Security and clarity matter more than speed.

An AI assistant must make focused, verifiable changes and must not introduce hidden security regressions, unnecessary abstractions, or unreviewed dependencies.

---

## 2. Required Context Before Any Code Change

Before editing, inspect:

1. `README.md`
2. `AGENTS.md`
3. `ARCHITECTURE.md`
4. `AI_RULES.md`
5. `AGENT_CONTRIBUTING.md`
6. `SECURITY.md`
7. Existing human contributor guide: `CONTRIBUTING.md`
8. `src/main/resources/application.properties`
9. The exact affected source files

Do not rely on assumptions from other Spring Boot projects.

Do not overwrite the existing human-facing `CONTRIBUTING.md`. AI-agent workflow rules belong in `AGENT_CONTRIBUTING.md`.

---

## 3. Hard Rules

### 3.1 Do Not Invent Project Structure

Only use packages and patterns that exist unless the task explicitly requires a new package.

Current top-level packages under `com.auth` are:

- `config`
- `controllers`
- `dtos`
- `entities`
- `exceptions`
- `repositories`
- `security`
- `services`

### 3.2 Keep Diffs Focused

Do not refactor unrelated files.
Do not reformat whole files unless formatting is the task.
Do not rename classes, packages, or endpoints without explicit instruction.

### 3.3 Preserve Stateless Security

The app uses stateless JWT authentication. Do not introduce server sessions, form login, or cookie-based auth unless the task explicitly asks for a new security model.

### 3.4 Protect New Endpoints

Every new endpoint must be intentionally classified as:

- Public
- Authenticated
- Admin-only
- Role-specific

Use `SecurityConfig` and/or `@PreAuthorize` clearly.

### 3.5 Keep Role Names Consistent

Existing authorities use:

```text
ROLE_USER
ROLE_ADMIN
```

Do not switch to `USER` / `ADMIN` or mix naming styles unless performing a complete documented migration.

### 3.6 Never Weaken Password Security

Passwords must be hashed with `PasswordEncoder` / BCrypt.
Never store, log, return, or compare plain-text passwords outside the intended credential verification flow.

### 3.7 Never Leak Tokens

Do not log:

- Access tokens
- Refresh tokens
- Authorization headers
- JWT claims containing sensitive values

Do not return token internals in error responses.

### 3.8 Do Not Commit Secrets

Never add real values for:

- JWT signing secrets
- Database passwords
- API keys
- Private keys
- Production credentials

Use placeholders or environment variables.

### 3.9 Do Not Return `null` From Controllers

Controllers must return meaningful responses or allow exceptions to be handled centrally.

Bad:

```java
return null;
```

Better:

```java
return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
```

or throw a meaningful exception handled by `GlobalExceptionHandler`.

### 3.10 Keep Exceptions Centralized

Use existing custom exceptions and `GlobalExceptionHandler` patterns. Do not scatter ad-hoc `try/catch` blocks unless they add real value.

---

## 4. Dependency Rules

Do not add dependencies casually.

A new dependency must have:

- A clear reason
- A security/reliability benefit
- No overlap with existing Spring Boot capability
- Documentation in the PR summary

Preferred Spring-native additions for future improvements:

- `spring-boot-starter-validation` for DTO validation
- Flyway or Liquibase for database migrations

Avoid adding large frameworks for small tasks.

---

## 5. API Rules

When changing API behavior:

1. Update DTOs deliberately.
2. Keep response shapes consistent.
3. Avoid exposing entities directly.
4. Update README or API documentation.
5. Update the Postman collection if endpoint contracts change.
6. Add tests for success and failure cases.

---

## 6. Testing Rules

Security-sensitive changes require tests.

Add or update tests for:

- Registration
- Duplicate username/email handling
- Login failure and success
- Password hashing
- JWT validation
- Invalid/expired token behavior
- Refresh-token success/failure
- Logout behavior
- Role-based access control
- Admin-only endpoints
- Global exception responses

Before completion, run:

```bash
mvn test
```

If tests cannot be run, explicitly state why.

---

## 7. Documentation Rules

Update documentation when changing:

- Setup instructions
- Config keys
- Environment variables
- Endpoint paths
- Request/response payloads
- Roles and permissions
- Authentication flow
- Refresh-token behavior
- Database schema assumptions

Do not leave docs stale after behavior changes.

---

## 8. Safe Change Patterns

Preferred patterns:

- Add DTO validation using Bean Validation.
- Move duplicated response creation into small helpers only when needed.
- Add service methods instead of putting logic in controllers.
- Add tests around security behavior.
- Introduce profiles for development and production configuration.
- Move demo seed data behind a development profile.
- Replace `ddl-auto=update` with migrations when productionizing.

---

## 9. Risky Change Patterns

Treat these as high-risk and call them out clearly:

- Editing `SecurityConfig`
- Editing `JwtAuthenticationFilter`
- Editing `JwtUtils`
- Editing `RefreshTokenService`
- Changing token expiry or signing behavior
- Changing role names or authority mapping
- Changing user-role entity relationships
- Making endpoints public
- Changing exception handling for auth failures
- Changing password hashing behavior

---

## 10. Blocked Changes Unless Explicitly Requested

Do not do these without explicit task requirements:

- Replace JWT with session authentication.
- Add OAuth/social login.
- Add a frontend.
- Rename the root package.
- Replace MySQL with another database.
- Replace Maven with Gradle.
- Remove Spring Security.
- Store refresh tokens only on the client.
- Disable authentication for protected user endpoints.
- Add hard-coded production credentials.

---

## 11. AI Response Format After Making Changes

Every AI coding response should include:

```text
Summary
- Clear explanation of what changed.

Changed files
- File path — reason

Validation
- Command run
- Result

Security notes
- Any auth, token, password, role, or config impact

Follow-up
- Any remaining work or recommended next step
```

Do not claim tests passed unless they were actually run.

---

## 12. Definition of Done for AI-Assisted Work

A task is done only when:

- The change is scoped to the request.
- The implementation matches the architecture.
- No secrets are introduced.
- Security is not weakened.
- New endpoints have explicit access rules.
- Sensitive fields are not leaked.
- Tests are added or updated for behavior changes.
- `mvn test` passes or the reason for not running it is stated.
- Documentation is updated when contracts or setup change.
