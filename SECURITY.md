# Security Model

This document outlines the security architecture and guarantees of AuthKit-Lite. 

## Authentication & Authorization
- **Passwords**: Passwords are hashed using **BCrypt** with an appropriate work factor (managed via Spring Security's `PasswordEncoder`). Passwords are never logged or returned in DTOs.
- **Minimum Password Policy**: The baseline requirement for registration is a 12-character passphrase.
- **JWT (JSON Web Token)**: 
  - Access tokens are stateless, signed using **HMAC SHA-256 (HS256)**, and short-lived (e.g., 15 minutes).
  - Validation includes signature verification, expiry time, `issuer` matching, and `audience` matching.
  - The JWT secret must be at least 32 characters long. The application enforces this at startup.
- **Passkeys / WebAuthn**:
  - The application relies entirely on Spring Security's native WebAuthn implementation for handling cryptographic assertions.
  - No biometric data is ever sent, processed, or stored by the application. The authenticator handles user verification locally.
  - The WebAuthn configuration implements strict Relaying Party (RP) ID validation to prevent phishing.

## Refresh Tokens & Session Lifecycle
- **Refresh Token Hashing**: Refresh tokens are opaque cryptographically secure random bytes sent to the client as URL-safe Base64 strings. We **do not store the raw token in the database**. Instead, we store a **SHA-256 hash** of the token. This prevents an attacker who compromises the database from hijacking active sessions.
- **One Active Session**: AuthKit-Lite maintains one active refresh session per user. A new login replaces the user's previous refresh token.
- **Token Rotation**: Every time a refresh token is used to obtain a new access token, it is immediately revoked and a new refresh token is issued. 
- **Concurrency Protection**: Rotation uses an atomic delete operation. If two concurrent requests attempt to refresh using the same token, only one will succeed, mitigating race-condition replay attacks.
- **Disabled Users**: Refresh token requests check if the associated user account is still enabled (`user.isEnabled()`). If disabled, the request is rejected and the token is revoked.
- **Logout Revocation**: Logout atomically deletes the hashed refresh token from the database, permanently ending the session.

## Secrets Management
- AuthKit-Lite strictly avoids hardcoded secrets. All sensitive configuration parameters (e.g., database credentials, JWT secrets) are loaded from environment variables (e.g. `${JWT_SECRET}`).

## Development Data & Demo Users
- The application includes an optional development profile (`dev`) that seeds demo users (`admin/admin123` and `user/user123`) via `DemoDataInitializer`. 
- **Do not enable the `dev` profile in production.** The production profile only creates baseline roles (`ROLE_USER` and `ROLE_ADMIN`) using Flyway.

## Rate Limiting & Brute Force Protection (Deployment Responsibility)
AuthKit-Lite focuses purely on standard token-based authentication. **It does not implement application-level distributed rate limiting.**

Production deployments are responsible for applying rate limiting and brute force protection at the infrastructure edge:
- API Gateway
- Reverse Proxy (e.g., Nginx, Traefik)
- Web Application Firewall (WAF)
- Load Balancer

## Reporting Security Vulnerabilities
This repository is a boilerplate. If you find a security vulnerability, please report it via standard GitHub issues or security advisories as per the repository maintainers' guidelines. Do not report issues related to your own deployed instances.
