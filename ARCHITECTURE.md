# AuthKit-Lite Architecture

This document describes the final target architecture of AuthKit-Lite based on Spring Boot 4.1.0 and Java 21.

## Core Design Philosophy

AuthKit-Lite adheres strictly to the rule:
> **Spring owns framework infrastructure. AuthKit owns only application-specific authentication behavior.**

This means we do not write custom JWT parsing filters, manual token validators, or manual `SecurityContext` populators. Instead, we configure Spring Security OAuth2 Resource Server to do the heavy lifting, providing a hardened, stateless, and standard-compliant authentication flow.

---

## 1. Login Flow

When a user submits their credentials (`LoginRequest`), the flow is:

1. **`AuthController`**: Receives the request and calls `AuthService.login(req)`.
2. **`AuthService`**: Delegates credential verification to `AuthenticationManager`.
3. **`AuthenticationManager`** → **`DaoAuthenticationProvider`**: Uses the `DatabaseUserDetailsService` to fetch the user and `PasswordEncoder` (BCrypt) to verify the password securely.
4. **Token Generation**: Upon successful authentication:
   - `TokenService` uses `JwtEncoder` to generate a short-lived stateless JWT access token.
   - `RefreshTokenService` generates a random secure bytes, encodes to URL-safe Base64, hashes it via SHA-256, and stores the hash (`token_hash`) in the `refresh_tokens` table. It also atomically deletes any existing refresh tokens for the user, maintaining a one-active-session policy.
5. **Response**: Returns both the JWT and the raw refresh token to the client.

---

## 2. Authenticated Request Flow

When the client makes a request to a protected endpoint, they send the JWT in the `Authorization: Bearer <token>` header:

1. **Spring Security Resource Server**: Intercepts the request.
2. **`JwtDecoder`**: 
   - Validates the token's cryptographic signature using the secret key (HS256).
   - Validates standard claims (expiry, issuer, audience).
3. **`JwtAuthenticationConverter`**: Reads the `roles` claim and maps them to Spring Security `GrantedAuthority` objects (e.g., `ROLE_USER`).
4. **`SecurityContext`**: Spring automatically populates the security context.
5. **Controller**: Endpoint logic runs. Authorization is enforced using `@PreAuthorize("hasAuthority(...)")`.

---

## 3. Refresh Token Rotation

When the access token expires, the client submits their raw refresh token to `/api/auth/refresh`:

1. **Hashing & Lookup**: `RefreshTokenService` hashes the raw token (SHA-256) and performs an atomic lookup/delete against the database (`DELETE FROM RefreshToken r WHERE r.tokenHash = :tokenHash`).
2. **Concurrency Protection**: The atomic delete ensures that if two requests attempt to use the same token simultaneously, only one succeeds.
3. **Validation**: The service verifies the token hasn't expired and the associated user account is still enabled (`user.isEnabled()`).
4. **Rotation**: A new raw refresh token is generated, hashed, and persisted. A new access token is generated.
5. **Response**: The client receives the new access and refresh tokens.

---

## 4. WebAuthn & Passkey Flow

To support modern passwordless authentication, AuthKit-Lite integrates Spring Security's native WebAuthn (Passkey) support.

1. **Dual Filter Chain Design**:
   - `/api/**`: Strictly stateless. Assumes standard JWT Bearer tokens and disables CSRF.
   - `/webauthn/**`: Uses temporary sessions exclusively to maintain the WebAuthn ceremony state (options, challenge, etc.) and relies on `CookieCsrfTokenRepository` for CSRF protection since WebAuthn calls are made directly from frontend JavaScript.
2. **Credential Persistence**:
   - We use Spring Security's native `JdbcUserCredentialRepository` and `JdbcPublicKeyCredentialUserEntityRepository`.
   - The database contains `webauthn_credentials` and `webauthn_user_entity` tables matching the Spring Security defaults, dropping the need for custom JPA entities.
3. **Passkey Management**:
   - `PasskeyController` allows authenticated users to list and delete their own passkeys using standard `UserCredentialRepository` lookups.

---

## 5. Database Schema & Migrations

- **Flyway**: We use a single clean baseline migration (`V1__init_schema.sql`) to initialize the database schema and baseline roles. 
- **Hibernate**: Configured to `validate` mode. It ensures the entity mappings perfectly match the Flyway schema.
- **Constraints**: Enforced rigidly (e.g., unique constraints on emails, usernames, and user-refresh-token relations).

---

## 5. Testing & Environment Isolation

- **Testcontainers**: Tests use an isolated MySQL container managed by Spring Boot 4.1.0's `@ServiceConnection` and Testcontainers integration. This guarantees tests never pollute or depend on the developer's local database.
- **MockMvc**: End-to-end integration tests use `MockMvc` to rigorously test API boundaries, assertions, validation, and JSON structures.
