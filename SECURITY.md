# Security Model

This document describes the protections AuthKit-Lite implements and the controls a production deployment must provide.

## Passwords and Accounts

- Registration requires a password of at least 12 characters.
- Spring Security's delegating `PasswordEncoder` hashes passwords with BCrypt before persistence.
- Passwords and password hashes are never returned by API DTOs and must never be logged.
- Disabled users cannot log in or refresh a session.

## JWT Access Tokens

- Access tokens are stateless JWTs signed with HMAC SHA-256 (HS256).
- Validation covers the signature, expiry, configured issuer, and configured audience.
- The signing secret is external configuration and must contain at least 32 characters; production deployments should use a high-entropy secret from a secret manager.
- Access-token lifetime is controlled by `JWT_ACCESS_TOKEN_TTL` and defaults to 15 minutes.
- Role authorities are read from the JWT `roles` claim and retain the `ROLE_USER` / `ROLE_ADMIN` naming used by Spring Security authorization.

Because access tokens are stateless, the application does not maintain an access-token denylist. A token that has already been issued remains valid until its expiration unless the signing secret changes.

## Refresh Tokens and Logout

- Refresh tokens are opaque random values. Only their SHA-256 hashes are stored in the database.
- AuthKit-Lite maintains one active refresh-token session per user; a new password or passkey login replaces the previous refresh token.
- Each successful refresh consumes the presented token and returns a new access/refresh pair.
- Rotation uses a pessimistic database lock. Concurrent reuse of the same refresh token allows one winner; subsequent replay receives `401 Unauthorized`.
- Expired refresh tokens are deleted when presented. A disabled user's refresh attempt is rejected and its token is deleted.
- Logout idempotently deletes the supplied refresh-token hash and prevents that session from issuing more access tokens.

Logout does not revoke an already-issued stateless access token. That access token remains valid until its configured expiration time.

## Authorization

- `/api/auth/register`, `/api/auth/login`, and `/api/auth/refresh` are public.
- Logout and all other `/api/**` endpoints require a valid Bearer token.
- `GET /api/users` requires `ROLE_ADMIN`; user profile and passkey-list endpoints require authentication.
- Actuator discovery, health, and info are public. Other unmatched routes are denied.

## CORS and CSRF

- Allowed origins come from `PASSKEY_ALLOWED_ORIGINS`; arbitrary origins are not reflected.
- Credentials are allowed only for configured origins. Production origins should use HTTPS.
- The Bearer-token `/api/**` chain is stateless and does not use browser cookies for authentication, so CSRF is disabled only for that chain.
- WebAuthn operations use temporary HTTP-session ceremony state and Spring Security's cookie CSRF repository. State-changing WebAuthn requests require the matching CSRF cookie/header and ceremony session.

## Passkeys / WebAuthn

- Passkeys are disabled by default and can be enabled through application properties or `.env` overrides.
- Spring Security performs WebAuthn option handling, challenge verification, cryptographic assertion verification, and credential ownership checks.
- Credentials and user entities use Spring Security's JDBC repositories.
- Registration and credential deletion require JWT authentication plus WebAuthn session/CSRF state. Authentication options and assertion submission are public but remain CSRF-protected.
- The relying-party ID and allowed origins are validated configuration. Production values must match the deployed HTTPS hostname.
- Authenticators perform biometric or PIN verification locally; the application does not receive or store biometric data.

## Development Features

- `DemoDataInitializer` is enabled by default for instant local startup and runs only when the connected database contains no users. Set `authkit.demo-data.enabled=false` (or `AUTHKIT_DEMO_DATA_ENABLED=false`) in every production environment.
- The browser API test frontend and its static assets under `/api-test/**` are intentionally public and require no authentication. Opening the frontend does not grant access to protected API endpoints.
- WebAuthn routes are denied when passkeys are disabled.
- Protected requests initiated by the frontend retain their normal JWT, role, CSRF, origin, and credential-ownership checks.
- Never enable demo-data seeding or use demo credentials in production.

## Secrets and Deployment Responsibilities

- `.env` is local-only and ignored. `.env.example` contains placeholders and safe defaults only.
- Do not commit database credentials, JWT secrets, tokens, private keys, or production URLs containing credentials.
- The generated default JWT secret is suitable only for a single local process and changes on restart. Production deployments must provide a stable `JWT_SECRET`.
- Use HTTPS/TLS for every production request and store secrets in the deployment platform's secret manager.
- AuthKit-Lite does not implement application-level rate limiting or distributed brute-force protection. Apply those controls at an API gateway, reverse proxy, load balancer, or web application firewall.
- Back up and monitor the database, restrict database permissions, and align the platform's termination grace period with `SHUTDOWN_TIMEOUT`.

## Reporting a Vulnerability

Do not disclose vulnerability details in a public issue. Use the repository's private vulnerability-reporting or GitHub Security Advisory channel when available. If no private channel is published, contact the maintainers privately through their verified project profile before sharing reproduction details.
