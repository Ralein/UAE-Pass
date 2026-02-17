# UAE Digital Identity Platform — Compliance Mapping

## ISO 27001 Controls

| Control | Section | Implementation | File |
|---------|---------|---------------|------|
| Access Control | A.9.4.2 | Argon2id PIN hashing, MFA (OTP + PIN) | `PinService.java`, `OtpService.java` |
| Cryptographic Controls | A.10.1.1 | AES-256-GCM (PII), RS256 (JWT), SHA-256 (identifiers) | `CryptoService.java` |
| Key Management | A.10.1.2 | PKCS12 keystore, rotation scheduler, SecretsProvider abstraction | `JwkRotationScheduler.java` |
| Audit Logging | A.12.4.1 | Append-only audit with HMAC chain, PII masking | `AuditService.java` |
| Network Security | A.13.1.1 | CORS, CSRF, rate limiting, session management | `SecurityConfig.java`, `RateLimitConfig.java` |
| Information Transfer | A.13.2.1 | TLS enforced, HSTS, Secure cookies | `SecurityConfig.java`, `application.yml` |
| Secure Development | A.14.2.1 | Input validation, SQL injection prevention (JPA), XSS prevention (CSP) | `GlobalExceptionHandler.java`, `middleware.ts` |
| Incident Management | A.16.1.4 | Risk scoring, anomaly detection, lockout policies | `RiskScoringService.java`, `RedisSecurityService.java` |
| Data Protection | A.18.1.4 | PII encryption at rest, hashed identifiers, data minimization in DTOs | `CryptoService.java`, DTOs |

## SOC 2 Trust Services Criteria

| Criteria | Category | Implementation |
|----------|----------|---------------|
| CC6.1 | Logical Access | Role-based access (ADMIN role for audit), JWT auth, session management |
| CC6.3 | Logical Access | Per-IP + per-user rate limiting, account lockout |
| CC6.6 | Logical Access | Secure cookie flags (HttpOnly, Secure, SameSite=Strict) |
| CC7.2 | System Operations | Micrometer metrics (auth success/failure, OTP rates, lockout counts) |
| CC7.3 | System Operations | Actuator health endpoints, Prometheus integration |
| CC8.1 | Change Management | Flyway migrations, Git version control |
| PI1.3 | Processing Integrity | Input validation (@Valid), fail-closed on security service unavailability |

## OAuth2 / OIDC Specification Compliance

| Spec Section | Requirement | Implementation |
|-------------|-------------|---------------|
| RFC 6749 §4.1 | Authorization Code Grant | Spring Authorization Server |
| RFC 6749 §10.12 | CSRF protection | CSRF double-submit cookie for browser clients |
| RFC 7636 | PKCE | Required for public clients |
| RFC 7519 | JWT | RS256 signed access tokens with `kid` header |
| RFC 7517 | JWK | Published at `/oauth2/jwks`, multiple keys |
| RFC 7662 | Token Introspection | `/oauth2/introspect` endpoint |
| RFC 7009 | Token Revocation | `/oauth2/revoke` endpoint |
| OpenID Connect Core | UserInfo | `/userinfo` endpoint |
| OpenID Connect Discovery | Provider metadata | `/.well-known/openid-configuration` |
| RFC 6749 §10.4 | Refresh token rotation | One-time-use with replay detection |
