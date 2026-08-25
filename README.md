# AuthKit-Lite

AuthKit-Lite is a lean, secure, and modern Spring Boot authentication boilerplate. It provides a robust starting point for REST APIs requiring JWT-based authentication, role-based authorization, and secure session management via refresh tokens.

## Tech Stack
- **Spring Boot 4.1.1**
- **Java 26**
- **Spring Security** (OAuth2 Resource Server)
- **Spring Data JPA** / **Hibernate**
- **MySQL** 
- **Flyway** (Database migrations)
- **Testcontainers** (Isolated integration testing)

## Core Features
- 🚀 **Stateless JWT Access Tokens**: Generated securely using Spring Security's native `JwtEncoder` and verified via `JwtDecoder` (HS256).
- 🔄 **Secure Refresh Token Rotation**: Refresh tokens are hashed via SHA-256 before storage. Concurrent refresh attempts are atomically protected. AuthKit-Lite maintains one active refresh session per user. A new login replaces the user's previous refresh token.
- 🔐 **Role-Based Authorization**: Endpoints are secured natively using Spring Security's `@PreAuthorize` (e.g., `ROLE_USER`, `ROLE_ADMIN`).
- 🔑 **Passkeys / WebAuthn**: Built-in support for biometric authentication, security keys, and device PINs leveraging Spring Security 7 WebAuthn integration.
- 🛑 **Configuration Validation**: Fails fast on startup if JWT or WebAuthn properties are misconfigured.
- 🐳 **Testcontainers Isolation**: A fully decoupled integration test suite that spins up an ephemeral MySQL container, keeping your local dev DB clean.

---

## Getting Started

### 1. Prerequisites
- Java 26
- MySQL 8.0+ (For running locally)
- Docker (Required for running tests via Testcontainers)

### 2. Configuration
Create a database in your MySQL instance (e.g., `authkit_db`). The application uses environment variables for secure configuration. You can export these or configure them in your IDE:

```bash
export DB_URL=jdbc:mysql://localhost:3306/authkit_db
export DB_USERNAME=root
export DB_PASSWORD=your_password
export JWT_SECRET=super-secure-secret-that-is-at-least-32-chars-long
export JWT_ISSUER=authkit
export JWT_AUDIENCE=authkit-api
```

### 3. Run the Application
The repository includes a Maven Wrapper, meaning you do not need Maven installed globally.

```bash
# Run locally (default profile)
./mvnw spring-boot:run

# Run with demo users seeded (dev profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

When running, Flyway applies `V1__init_schema.sql` followed by the Spring Security WebAuthn JDBC schema in `V2__add_webauthn.sql`. Hibernate then validates the JPA-managed tables.

### 4. Testing
Tests rely on Docker and Testcontainers to guarantee isolation.

```bash
# Run tests
./mvnw clean test

# Package the application
./mvnw clean verify
```

---

## API Endpoints

### Authentication
- `POST /api/auth/register`: Register a new user. Minimum password length is 12 characters.
- `POST /api/auth/login`: Authenticate and receive `accessToken` and `refreshToken`.
- `POST /api/auth/refresh`: Rotate refresh token and issue a new access token.
- `POST /api/auth/logout`: Revoke the refresh token. Requires the current Bearer access token.

### WebAuthn / Passkeys
- `GET /webauthn/csrf`: Obtain the `XSRF-TOKEN` cookie and the matching `X-XSRF-TOKEN` header value.
- `POST /webauthn/register/options`: Initiate passkey registration. Requires Bearer authentication, the CSRF cookie/header, and the same temporary session.
- `POST /webauthn/register`: Complete passkey registration. Requires Bearer authentication, the CSRF cookie/header, the temporary session, and browser-generated WebAuthn data.
- `DELETE /webauthn/register/{credentialId}`: Delete the authenticated user's credential through Spring Security's ownership-checked endpoint. Requires Bearer authentication and CSRF/session state.
- `POST /webauthn/authenticate/options`: Initiate passkey authentication. Public, but requires the CSRF cookie/header and temporary session.
- `POST /login/webauthn`: Complete passkey authentication. On success, returns AuthKit access and refresh tokens. Requires the CSRF cookie/header, temporary session, and browser-generated WebAuthn data.

### Users (Protected)
- `GET /api/users/me`: Fetch profile of the currently authenticated user.
- `GET /api/users/me/passkeys`: List all registered passkeys for the user.
- `GET /api/users`: Fetch a paginated list of all users (Requires `ROLE_ADMIN`).

### Operations
- `GET /actuator`: Public discovery links for the exposed actuator endpoints.
- `GET /actuator/health`: Public health status.
- `GET /actuator/info`: Public application information.
- `GET /api-test/index.html`: Dependency-free browser API test console.

The `/api/**` chain is stateless and uses Bearer tokens, so CSRF is disabled only for that chain. WebAuthn endpoints retain Spring Security's session-backed ceremony state and cookie CSRF protection. CORS credentials are accepted only from the origins configured by `authkit.passkey.allowed-origins`.

---

## Postman Collection
An up-to-date Postman collection (`Auth-Kit API Collection.postman_collection.json`) is included in the root directory.
It creates a unique test user, chains access/refresh tokens, exercises CSRF and CORS, and contains assertions for every Postman-compatible endpoint. Run it sequentially against a fresh application started with the `dev` profile. Successful passkey enrollment/assertion and credential deletion remain browser-authenticator checks and are explicitly documented as excluded in the collection.

## Browser API Test Console

The project includes a zero-build static test client at `src/main/resources/static/api-test/index.html`. It can exercise every endpoint individually, run a combined JWT workflow, or run the complete workflow with a real browser passkey.

1. Start the application with the development profile so the administrator checks can use the seeded account:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

2. Double-click `src/main/resources/static/api-test/index.html`.
3. The local file automatically opens `http://localhost:8080/api-test/index.html`, because WebAuthn cannot operate from a `file://` origin.
4. In a current WebAuthn-capable browser, choose **Run complete browser journey** to test all API groups, including passkey creation, passkey login, and passkey deletion. Approve the authenticator prompts shown by the browser.
5. To test one endpoint, use its button in the relevant section. The state chips show which JWT, refresh token, CSRF session, and passkey prerequisites are ready.

The page has no package-manager or build step and loads no third-party scripts. Access and refresh tokens are kept only in page memory and are cleared when the tab closes or **New test identity** is selected. The default WebAuthn configuration expects the hostname `localhost`; do not replace it with `127.0.0.1` unless the RP ID and allowed origins are deliberately reconfigured.

## Production Considerations
Before deploying to production:
1. Ensure the `dev` profile is disabled to prevent seeding demo credentials.
2. Provide a highly entropic, securely managed `JWT_SECRET`.
3. Implement **rate limiting** at your API Gateway or reverse proxy, as this application focuses purely on authentication logic and does not implement application-level throttling.
4. Host over HTTPS/TLS to protect bearer tokens in transit.
5. Remove or restrict `/api-test/**` in deployments where a public developer test console is not desired. The console does not bypass endpoint security, but it is intended for local verification.

## Documentation Reference
- [Architecture Details](ARCHITECTURE.md)
- [Security Guarantees](SECURITY.md)
