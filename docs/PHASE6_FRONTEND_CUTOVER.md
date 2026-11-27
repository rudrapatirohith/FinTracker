# Phase 6 — Frontend cutover

## Runtime configuration

Frontend:

- `NEXT_PUBLIC_CORE_API_URL`: absolute origin of the Spring Boot API (for local development, `http://localhost:8080`).

Backend:

- `FRONTEND_ORIGIN`: exact frontend origin allowed by the API CORS policy (for local development, `http://localhost:3000`).
- `FINTRACKER_AI_GEMINI_API_KEY`: optional server-only Gemini credential. Do not use a `NEXT_PUBLIC_*` variable for this secret. If it is not configured, the assistant endpoint returns HTTP 503 and the frontend displays the backend message.
- Keep refresh-cookie settings appropriate for the deployment. Production must use HTTPS and `REFRESH_COOKIE_SECURE=true`.

## Authentication and API contract

- The frontend keeps the short-lived access JWT in AuthProvider memory only.
- The refresh token remains in the backend-managed HttpOnly cookie.
- The shared API client attaches bearer authentication and obtains a CSRF token for mutations.
- On an expired access token, concurrent requests share one refresh operation; the refreshed token is validated against `/api/v1/auth/me`, then the original request is retried once.
- Financial record ownership is always resolved from the authenticated JWT. The browser must not send a trusted owner/user ID.
- Spring Data page responses are consumed through the shared `apiGetPage` helper.

## Financial screens

The dashboard, income, expenses, monthly expenses, loans, loan payments, transfers, scheduled payments, analytics, history, and reports now use Spring Boot endpoints.

Loan payments remain nested under a loan. The backend service owns the transaction that records a payment and adjusts the parent loan balance; the frontend must not issue a second balance update.

The standalone expenses screen is backed by the existing `monthly_expenses` resource because the Phase 5 migration did not include a separate legacy `expenses` table. Both expense views therefore operate on the same canonical records.

## AI assistant

The browser no longer calls Gemini directly or receives a provider key. It calls the authenticated `POST /api/v1/ai/chat` endpoint. The backend builds the financial context from owner-scoped reporting queries and calls Gemini using the server-only `FINTRACKER_AI_GEMINI_API_KEY`.

## Password recovery after the migration

Supabase password hashes and active sessions were not migrated. Google-authenticated users can continue through Google OAuth. Email/password users can set a new password through the one-time recovery flow. The backend stores only a SHA-256 token digest, expires links after 30 minutes, invalidates previous links, and revokes all active refresh sessions after a successful reset. The reset endpoints return the same accepted response for unknown email addresses to reduce account enumeration.

Configure SMTP before enabling this flow in a deployment: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, and `FRONTEND_PASSWORD_RESET_URL`. Without SMTP configuration the API returns HTTP 503 and does not claim an email was sent.

## Verification

CI runs:

- Core API: Java 21, PostgreSQL-backed Maven verification.
- Frontend: `npm ci`, ESLint, TypeScript, and Next.js production build.

A passing CI build does not replace local browser/API verification against the user's PostgreSQL database. Confirm CRUD, cross-user isolation, refresh rotation, CSRF, and empty/loading/error states before production cutover.
