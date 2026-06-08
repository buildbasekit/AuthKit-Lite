# AGENT_CONTRIBUTING.md — AI Agent Contribution Workflow for AuthKit-Lite

This file is for AI coding agents and AI-assisted development workflows. It does not replace the repository's existing human-facing `CONTRIBUTING.md`. Human contributors should continue using `CONTRIBUTING.md`; agents should use this file together with `AGENTS.md`, `ARCHITECTURE.md`, and `AI_RULES.md`.

AuthKit-Lite is a compact Spring Boot JWT authentication starter, so the execution priority is simple: keep it secure, clean, readable, and easy to extend.

---

## 0. Placement Rule

Save this file as:

```text
AGENT_CONTRIBUTING.md
```

Do not save this content as `CONTRIBUTING.md`, because that file already exists for human contributors.

---

## 1. Prerequisites

Before running the project locally, install:

- Java 21
- Maven 3.9+
- MySQL 8+ or a compatible MySQL database
- Git
- An IDE such as IntelliJ IDEA, Eclipse, or VS Code

---

## 2. Local Setup

### 2.1 Clone the repository

```bash
git clone https://github.com/buildbasekit/AuthKit-Lite.git
cd AuthKit-Lite
```

### 2.2 Create a local database

Create a MySQL database for local development. Example:

```sql
CREATE DATABASE authkit_lite;
```

### 2.3 Configure application properties

Update:

```text
src/main/resources/application.properties
```

Set your local database values and a long random JWT secret.

Example shape:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/authkit_lite
spring.datasource.username=your_local_username
spring.datasource.password=your_local_password

app.jwt.secret=replace-with-a-long-random-local-development-secret
```

Do not commit real credentials or production secrets.

### 2.4 Run the app

```bash
mvn spring-boot:run
```

### 2.5 Run tests

```bash
mvn test
```

---

## 3. Branch Naming

Use clear branch names:

```text
feature/add-email-verification
fix/logout-error-response
security/harden-jwt-validation
docs/update-api-guide
test/add-rbac-tests
```

---

## 4. Commit Guidelines

Use concise, action-based commits:

```text
feat: add email verification token entity
fix: return proper response on logout failure
test: add admin endpoint authorization tests
docs: document refresh token lifecycle
refactor: move user dto mapping into service
```

Keep commits focused. Do not mix unrelated formatting, refactoring, and feature work in one commit.

---

## 5. Coding Standards

### Java and Spring

- Use Java 21 features only when they improve clarity.
- Follow Spring Boot 3 conventions.
- Prefer constructor injection.
- Keep controllers thin.
- Keep business logic in services.
- Keep persistence logic in repositories.
- Keep authentication/token logic in the `security` package.
- Use DTOs for request and response payloads.
- Avoid exposing JPA entities directly through public APIs.

### Lombok

Lombok is allowed because the project already uses it. Use it for simple DTOs and entities where it improves readability. Avoid hiding complex behavior behind annotations.

### Formatting

- Keep code consistently formatted.
- Avoid large formatting-only diffs.
- Do not reorganize the whole project unless the issue explicitly requires it.

---

## 6. Security Contribution Rules

AuthKit-Lite is an authentication starter. Security mistakes here get copied into other applications, so treat security changes with extra discipline.

Never:

- Commit real secrets.
- Log access tokens or refresh tokens.
- Log raw passwords.
- Store plain-text passwords.
- Bypass BCrypt hashing.
- Disable token validation to make tests pass.
- Expose password fields in DTOs.
- Add public endpoints by accident.
- Return stack traces or sensitive implementation details to clients.

Always:

- Use `PasswordEncoder` for password hashing.
- Protect new endpoints intentionally.
- Preserve the `ROLE_*` authority format unless performing a full migration.
- Add tests for authentication and authorization changes.
- Keep demo credentials development-only.

---

## 7. Testing Expectations

Run this before opening a pull request:

```bash
mvn clean test
```

Add or update tests when changing:

- Registration
- Login
- Password hashing
- JWT generation or validation
- Refresh-token lifecycle
- Logout behavior
- Role checks
- Security configuration
- Exception handling
- User profile/admin endpoints

For security-related changes, a PR without tests should be considered incomplete unless there is a clear reason.

---

## 8. API and Documentation Updates

Update documentation when changing:

- Endpoint paths
- Request/response bodies
- Role requirements
- JWT settings
- Refresh-token behavior
- Local setup steps
- Database requirements
- Environment variables or application properties

Also update the Postman collection if the API contract changes.

---

## 9. Pull Request Checklist

Before requesting review, verify:

- [ ] The branch has a focused purpose.
- [ ] Code follows the existing package structure.
- [ ] Controllers remain thin.
- [ ] Services contain business/security workflow logic.
- [ ] Repositories only handle persistence.
- [ ] New endpoints are protected intentionally.
- [ ] DTOs do not expose sensitive fields.
- [ ] `mvn test` passes.
- [ ] Security-sensitive changes include tests.
- [ ] Documentation is updated where needed.
- [ ] No real secrets or local credentials are committed.
- [ ] The diff does not include unrelated formatting churn.

---

## 10. Reporting Security Issues

Do not open public issues for vulnerabilities.

Use the process described in `SECURITY.md`. Include enough detail to reproduce the issue, but avoid sharing exploit details publicly.

---

## 11. AI-Assisted Contributions

AI-generated code is allowed, but the contributor is responsible for review and validation.

When using AI assistance:

- Read `AGENTS.md` and `AI_RULES.md` first.
- Review every generated line.
- Run tests locally.
- Remove hallucinated classes, dependencies, or configuration.
- Do not accept AI changes that weaken security.
- Clearly mention AI-assisted areas in the PR description if useful for review.


---

## Agent Operating Flow

Before changing code, an AI agent must:

1. Restate the requested change in one sentence.
2. Identify the affected package and files.
3. Check whether the change touches authentication, authorization, JWT, refresh tokens, password handling, roles, or exceptions.
4. Make the smallest safe change.
5. Run or document the relevant validation command.
6. Summarize the changed files and any remaining risks.

For security-sensitive changes, prefer an explicit, boring implementation over a clever abstraction.

---

## Agent Output Standard

When an AI agent finishes a task, the final response should include:

- What changed.
- Files changed.
- Tests or validation performed.
- Security impact, if any.
- Any follow-up work that should be handled separately.

Do not claim tests passed unless they were actually run.
