# AuthKit-Lite

AuthKit-Lite is a lean, secure, and modern Spring Boot authentication boilerplate. It provides a robust starting point for REST APIs requiring JWT-based authentication, role-based authorization, and secure session management via refresh tokens.

## Tech Stack
- **Spring Boot 4.1.0**
- **Java 21**
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
- Java 21+
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

When running, Flyway will automatically apply the baseline schema (`V1__init_schema.sql`).

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
- `POST /api/auth/logout`: Revoke the refresh token.

### WebAuthn / Passkeys
- `POST /webauthn/register/options`: Initiate passkey registration.
- `POST /webauthn/register`: Complete passkey registration.
- `POST /webauthn/authenticate/options`: Initiate passkey authentication.
- `POST /webauthn/authenticate`: Complete passkey authentication.

### Users (Protected)
- `GET /api/users/me`: Fetch profile of the currently authenticated user.
- `GET /api/users/me/passkeys`: List all registered passkeys for the user.
- `DELETE /api/users/me/passkeys/{id}`: Delete a specific passkey.
- `GET /api/users`: Fetch a paginated list of all users (Requires `ROLE_ADMIN`).

---

## Postman Collection
An up-to-date Postman collection (`Auth-Kit API Collection.postman_collection.json`) is included in the root directory.
It features automatic pre-request and test scripts that extract tokens on login/refresh and inject them into your Postman environment.

## Production Considerations
Before deploying to production:
1. Ensure the `dev` profile is disabled to prevent seeding demo credentials.
2. Provide a highly entropic, securely managed `JWT_SECRET`.
3. Implement **rate limiting** at your API Gateway or reverse proxy, as this application focuses purely on authentication logic and does not implement application-level throttling.
4. Host over HTTPS/TLS to protect bearer tokens in transit.

## Documentation Reference
- [Architecture Details](ARCHITECTURE.md)
- [Security Guarantees](SECURITY.md)
