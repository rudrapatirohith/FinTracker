# FinTracker Core API — JWT Architecture & Lifecycle

This document outlines how `JwtConfig` and `JwtTokenService` collaborate to issue and validate short-lived access tokens (`HS256`).

---

## 1. Visual Flowchart (Renders Automatically on GitHub & VS Code with Mermaid)

```mermaid
flowchart TD
    %% Styling
    classDef config fill:#1e293b,stroke:#38bdf8,stroke-width:2px,color:#f8fafc
    classDef service fill:#1e1b4b,stroke:#818cf8,stroke-width:2px,color:#f8fafc
    classDef process fill:#0f172a,stroke:#94a3b8,stroke-width:1px,color:#e2e8f0
    classDef decision fill:#312e81,stroke:#c084fc,stroke-width:2px,color:#f8fafc
    classDef success fill:#064e3b,stroke:#34d399,stroke-width:2px,color:#ecfdf5
    classDef error fill:#7f1d1d,stroke:#f87171,stroke-width:2px,color:#fef2f2

    subgraph Startup ["1. Application Startup (Spring Context Initialization)"]
        ENV[".env / application.properties<br/>(JWT_SECRET, issuer, access-token-ttl)"] --> KEY_BEAN["JwtConfig.jwtSecretKey()<br/>• Base64 decodes secret<br/>• Enforces >= 32 bytes (256-bit)<br/>• Creates HmacSHA256 SecretKeySpec"]:::config
        KEY_BEAN --> ENC_BEAN["JwtConfig.jwtEncoder()<br/>• Wraps SecretKey in ImmutableSecret<br/>• Exposes NimbusJwtEncoder bean"]:::config
        KEY_BEAN --> DEC_BEAN["JwtConfig.jwtDecoder()<br/>• Configures NimbusJwtDecoder (HS256)<br/>• Attaches JwtValidators.createDefaultWithIssuer(issuer)"]:::config
    end

    subgraph Issuance ["2. Token Creation Flow (Login)"]
        LOGIN["Login Service<br/>(User password verified)"] --> CALL_SVC["JwtTokenService.createAccessToken(UUID userId)"]:::service
        CALL_SVC --> NULL_CHK{"userId == null?"}:::decision
        NULL_CHK -- Yes --> ERR_ARG["Throws IllegalArgumentException"]:::error
        NULL_CHK -- No --> BUILD_CLAIMS["Build JwtClaimsSet (Clock.systemUTC)<br/>• iss: fintracker-core-api<br/>• sub: userId.toString() (UUID)<br/>• iat: issuedAt | exp: issuedAt + PT15M<br/>• jti: randomUUID()<br/>• token_use: 'access'"]:::process
        BUILD_CLAIMS --> SIGN["Build JwsHeader (HS256)<br/>& call jwtEncoder.encode(...)"]:::process
        ENC_BEAN -.->|Injected Bean| SIGN
        SIGN --> RETURN_JWT["Return signed JWT string<br/>(header.payload.signature)"]:::success
    end

    subgraph Verification ["3. Token Validation Flow (Protected Requests)"]
        REQ["Incoming API Request<br/>Authorization: Bearer &lt;token&gt;"] --> DECODE["JwtDecoder.decode(token)"]:::service
        DEC_BEAN -.->|Injected Bean| DECODE
        DECODE --> CHK_SIG{"1. Signature Valid?<br/>(Recomputed HMAC-SHA256<br/>matches SecretKey)"}:::decision
        CHK_SIG -- No --> ERR_JWT["Throws JwtException<br/>(401 Unauthorized)"]:::error
        CHK_SIG -- Yes --> CHK_CLAIMS{"2. Claims Valid?<br/>• Current UTC time < exp<br/>• iss == fintracker-core-api"}:::decision
        CHK_CLAIMS -- No --> ERR_JWT
        CHK_CLAIMS -- Yes --> GRANT["Returns validated Jwt object<br/>Access Granted"]:::success
    end
```

---

## 2. Universal ASCII Flowchart (Works in Every Editor & Terminal)

```text
================================================================================
 1. APPLICATION STARTUP (JwtConfig.java)
================================================================================
  [.env: JWT_SECRET] ──► Base64 Decode & Verify >= 32 bytes
                                │
                                ▼
                        [SecretKey Bean] (HmacSHA256)
                         ├──► Injects into ──► [JwtEncoder Bean] (NimbusJwtEncoder)
                         └──► Injects into ──► [JwtDecoder Bean] (NimbusJwtDecoder + Issuer Validator)

================================================================================
 2. TOKEN CREATION FLOW (JwtTokenService.java)
================================================================================
  [Login Verified]
         │
         ▼
  createAccessToken(UUID userId)
         ├──► If userId == null ──► Throw IllegalArgumentException
         │
         ▼
  Build JwtClaimsSet (Clock.systemUTC)
   • iss: "fintracker-core-api"
   • sub: userId.toString()
   • iat: now | exp: now + 15 mins (PT15M)
   • jti: randomUUID()
   • token_use: "access"
         │
         ▼
  [JwtEncoder] signs Header (HS256) + Claims using SecretKey
         │
         ▼
  Returns Signed JWT ("header.payload.signature")

================================================================================
 3. TOKEN VALIDATION FLOW (Protected Endpoints)
================================================================================
  [Incoming Request: Authorization: Bearer <token>]
         │
         ▼
  [JwtDecoder.decode(token)]
         ├──► 1. Signature Check (Recomputes HMAC-SHA256 with SecretKey)
         ├──► 2. Expiration Check (Current UTC time < exp)
         └──► 3. Issuer Check (iss == "fintracker-core-api")
                   │
                   ├──► Any Check Fails ──► Throw JwtException (401 Unauthorized)
                   └──► All Checks Pass ──► Return validated Jwt (Access Granted)
```

---

## 3. Component Summary

| Class | Role | Key Responsibilities |
| :--- | :--- | :--- |
| **`JwtConfig`** | Cryptographic Factory | • Reads Base64 `JWT_SECRET` from environment variables.<br>• Enforces a 256-bit (32-byte) minimum key length at startup.<br>• Registers singleton `SecretKey`, `JwtEncoder`, and `JwtDecoder` beans.<br>• Configures `JwtValidators.createDefaultWithIssuer(issuer)` to reject tokens from external issuers. |
| **`JwtTokenService`** | Token Builder | • Generates short-lived (`PT15M`) access tokens using `Clock.systemUTC()`.<br>• Sets immutable user `UUID` as `sub` instead of mutable email addresses.<br>• Adds unique `jti` IDs and `token_use: "access"` claims. |