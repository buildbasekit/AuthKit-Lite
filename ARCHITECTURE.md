# AuthKit-Lite Architecture

This document describes the AuthKit-Lite architecture based on Spring Boot 4.1.1 and Java 26.

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
   - `RefreshTokenService` generates secure random bytes, encodes them as URL-safe Base64, hashes the token via SHA-256, and stores only the hash (`token_hash`) in the `refresh_tokens` table. It deletes any existing refresh token for the user, maintaining a one-active-session policy.
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

1. **Hashing & Locking**: `RefreshTokenService` hashes the raw token (SHA-256) and loads the matching database row with a pessimistic write lock.
2. **Concurrency Protection**: Concurrent replay attempts serialize on that row. The winner consumes and replaces it; waiting requests then observe that the old row no longer exists and receive `401 Unauthorized`.
3. **Validation**: The service verifies the token hasn't expired and the associated user account is still enabled (`user.isEnabled()`).
4. **Rotation**: A new raw refresh token is generated, hashed, and persisted. A new access token is generated.
5. **Response**: The client receives the new access and refresh tokens.

---

## 4. WebAuthn & Passkey Flow

To support modern passwordless authentication, AuthKit-Lite integrates Spring Security's native WebAuthn (Passkey) support.

1. **Ordered Filter Chain Design**:
   - Order 1, `/webauthn/**` and `/login/webauthn`: Uses Spring Security WebAuthn, accepts JWT authentication for registration/credential management, keeps temporary ceremony state in the HTTP session, and uses `CookieCsrfTokenRepository`. Only CSRF retrieval, authentication options, and assertion submission are public.
   - Order 2, `/api/**`: Strictly stateless, authenticates standard JWT Bearer tokens, and disables CSRF because it does not use browser cookies for authentication.
   - Order 3, fallback: Permits the static `/api-test/**` browser test assets only when `authkit.test-console.enabled=true`, permits actuator discovery links plus the exposed `health` and `info` endpoints, and denies every other unmatched request. The console is disabled by default and enabled by the `dev` profile.
2. **Credential Persistence**:
   - We use Spring Security's native `JdbcUserCredentialRepository` and `JdbcPublicKeyCredentialUserEntityRepository`.
   - The database contains `user_credentials` and `user_entities`, using the exact column contract expected by Spring Security 7.1's JDBC repositories.
3. **Passkey Management**:
   - `PasskeyController` provides the application-specific credential-list DTO. Deletion uses Spring Security's native `DELETE /webauthn/register/{credentialId}` filter and ownership authorization manager.

---

## 5. Database Schema & Migrations

- **Flyway**: `V1__init_schema.sql` creates application tables and baseline roles; `V2__add_webauthn.sql` adds Spring Security's JDBC WebAuthn tables. Flyway is the only schema owner.
- **Hibernate**: Configured to `validate` mode. It ensures the entity mappings perfectly match the Flyway schema.
- **Constraints**: Enforced rigidly (e.g., unique constraints on emails, usernames, and user-refresh-token relations).

---

## 6. Testing & Environment Isolation

- **Testcontainers**: Tests use an isolated MySQL 8.4 container managed by Spring Boot 4.1.1's `@ServiceConnection` and Testcontainers 2 integration. This guarantees tests never pollute or depend on the developer's local database.
- **MockMvc**: End-to-end integration tests use `MockMvc` to rigorously test API boundaries, assertions, validation, and JSON structures.
- **Browser API Test Console**: BuildBaseKit-branded static HTML, CSS, JavaScript, and logo assets under `src/main/resources/static/api-test/` provide a same-origin manual client without adding a frontend runtime or dependency. A locally double-clicked `index.html` acts only as a launcher and redirects to the server-hosted `localhost` copy so WebAuthn has a valid RP origin. The client preserves WebAuthn ceremony state through the browser session cookie, sends the cookie-backed CSRF token, delegates credential creation/assertion to `navigator.credentials`, and keeps JWT/refresh tokens only in memory. Security configuration denies the assets unless the console is explicitly enabled.
