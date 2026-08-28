# AuthKit-Lite by BuildBaseKit

AuthKit-Lite is a compact Spring Boot authentication boilerplate for REST APIs. It combines stateless JWT access tokens, rotating refresh tokens, role-based authorization, and optional passkeys without adding a separate frontend or custom JWT filter.

## Technology

- Spring Boot 4.1.1 and Java 25 LTS
- Spring Security with OAuth2 Resource Server and native WebAuthn support
- Spring Data JPA, Hibernate, H2/MySQL, and Flyway
- BCrypt password hashing
- Testcontainers with MySQL 8.4 for integration tests

## Core Features

- HS256 JWT access tokens validated for signature, expiry, issuer, and audience
- Opaque refresh tokens stored only as SHA-256 hashes and rotated atomically
- One active refresh-token session per user, including concurrent replay protection
- `ROLE_USER` and `ROLE_ADMIN` authorization
- Optional WebAuthn/passkey registration, authentication, listing, and deletion
- Graceful shutdown, health/info actuator endpoints, and configuration validation
- Configurable demo users and a dependency-free browser API console

## Requirements

- Java 25 LTS
- MySQL 8.0 or later only when using an external MySQL database
- Docker or another Testcontainers-compatible container runtime for tests
- Git

The Maven wrapper is included, so a system Maven installation is not required.

## Getting Started

1. Clone the repository and enter its directory.
2. Run `./mvnw spring-boot:run`.

The default configuration uses an in-memory H2 database, applies the Flyway migrations, and creates the demo users below when the database has no users. No database setup is required.

- Administrator: `admin` / `password123123`
- User: `user` / `password123123`

## Environment Configuration

All runnable defaults are in `src/main/resources/application.properties`. To override them, copy [.env.example](.env.example) to `.env`, uncomment only the settings you need, and restart the application. The root `.env` file is loaded automatically and is ignored by Git.

Database variables are optional: setting `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` switches the application to the configured database. Set `AUTHKIT_DEMO_DATA_ENABLED=false` to disable dummy users. When dummy data is enabled, it is inserted only if the connected database's `users` table is empty.

For zero-configuration local runs, the application generates an ephemeral JWT secret at startup. Production deployments must provide a stable, high-entropy `JWT_SECRET` of at least 32 characters, use an external database, and disable demo data. For production passkeys, use an HTTPS origin and configure the relying-party ID and allowed origins to match the deployed hostname. Never commit `.env` or production credentials.

## Running Locally

Run the default configuration:

```bash
./mvnw spring-boot:run
```

Flyway creates and migrates the in-memory H2 schema; Hibernate validates it and never owns schema changes. To use MySQL instead, provide the database environment variables shown in [.env.example](.env.example).

## Testing

The integration suite starts an isolated MySQL 8.4 container and does not use the local development database.

```bash
./mvnw clean verify
```

Tests cover registration, password login, JWT validation, refresh-token rotation and replay, concurrent refresh handling, logout, disabled users, RBAC, CORS, CSRF, actuator security, WebAuthn option/failure paths, and default development-tool isolation. A real browser/platform authenticator is still required to complete successful WebAuthn ceremonies.

## API Overview

### Authentication

- `POST /api/auth/register` — register a user; passwords require at least 12 characters.
- `POST /api/auth/login` — receive an access token and refresh token.
- `POST /api/auth/refresh` — rotate a refresh token and receive a new token pair.
- `POST /api/auth/logout` — invalidate the supplied refresh-token session; requires Bearer authentication.

### Users

- `GET /api/users/me` — current authenticated user profile.
- `GET /api/users/me/passkeys` — current user's passkey metadata.
- `GET /api/users` — paginated user list for `ROLE_ADMIN`.

### Passkeys / WebAuthn

- `GET /webauthn/csrf` — obtain the CSRF cookie and token for a ceremony session.
- `POST /webauthn/register/options` — authenticated registration options.
- `POST /webauthn/register` — complete authenticated registration with browser data.
- `DELETE /webauthn/register/{credentialId}` — delete an owned credential.
- `POST /webauthn/authenticate/options` — public authentication options.
- `POST /login/webauthn` — complete browser authentication and receive AuthKit tokens.

WebAuthn operations retain temporary HTTP-session ceremony state and cookie CSRF protection. The `/api/**` chain remains stateless and uses Bearer tokens, so CSRF is disabled only for that chain.

### Operations

- `GET /actuator`, `GET /actuator/health`, and `GET /actuator/info` are public.
- Other unmatched routes are denied.

The root-level [AuthKit-Lite-API.postman_collection.json](AuthKit-Lite-API.postman_collection.json) provides a guided API workflow. Run it sequentially against a fresh application. WebAuthn registration/assertion completion and credential deletion are marked as manual browser-authenticator operations rather than simulated successes.

## Security Behavior

Passwords are BCrypt-hashed. Access tokens are short-lived, stateless JWTs; refresh tokens are opaque, hashed at rest, single-session, and rotated under a database lock. Refresh replay, expired tokens, and refresh attempts for disabled users are rejected.

Logout invalidates the refresh-token session and prevents additional access tokens from being issued from that session. Any already-issued access token remains valid until its configured expiration time; AuthKit-Lite does not maintain an access-token denylist.

CORS accepts credentials only from configured origins. Production deployments must also provide HTTPS, secret management, and edge rate limiting/brute-force protection. See [SECURITY.md](SECURITY.md) for the precise security model.

## Optional Local Features

The API test frontend is always public at [http://localhost:8080/api-test](http://localhost:8080/api-test); no login or configuration flag is required to open it. The frontend does not bypass authentication for protected API operations.

To enable passkeys locally, add this override to `.env`:

```properties
PASSKEY_ENABLED=true
```

Never leave demo seeding enabled in production.

## Project Structure

```text
src/main/java/com/auth/
├── config/       # security and validated configuration
├── controllers/  # HTTP endpoints
├── dtos/         # request and response contracts
├── entities/     # JPA entities
├── exceptions/   # centralized API error handling
├── repositories/ # persistence interfaces
├── security/     # authentication and token lifecycle
└── services/     # user-facing business operations

src/main/resources/
├── db/migration/ # Flyway schema migrations
└── static/api-test/ # public browser API test frontend
```

Architecture details are in [ARCHITECTURE.md](ARCHITECTURE.md), human contribution guidance is in [CONTRIBUTING.md](CONTRIBUTING.md), and AI-agent constraints are in [AGENTS.md](AGENTS.md).

## License

AuthKit-Lite is available under the [MIT License](LICENSE).
