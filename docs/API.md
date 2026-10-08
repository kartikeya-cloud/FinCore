# FinCore API

Base path: `/api`. Requests use a same-origin session cookie. JSON responses are returned except logout (204). Fetch `/api/csrf` first and send its `token` under its returned `headerName` for POST requests. Fetch a new token after login/logout.

| Method | Path | Authentication | Purpose |
| --- | --- | --- | --- |
| GET | /csrf | Public | Get session CSRF token and header name |
| POST | /register | Public + CSRF | Create account and virtual opening funds |
| POST | /login | Public + CSRF | Form-encoded email/password; create session |
| POST | /logout | CSRF | Invalidate session |
| GET | /me | Session | Current account id, name, email |
| GET | /wallet | Session | Current user's wallet and integer balanceCents |
| GET | /summary | Session | All-time incomingCents, outgoingCents, categories |
| GET | /recipient?email=... | Session | Exact email recipient lookup |
| GET | /transactions?page=0&size=20 | Session | Newest-first history; size 1–100, page 0–100000 |
| GET | /transactions/{id}/receipt | Participant session | Transfer metadata and matching ledger entries |
| POST | /transfers | Session + CSRF | Post or replay a transfer |

`GET /actuator/health` is public and returns only health status.

## Registration

```json
{"name":"Kartikeya","email":"you@example.com","password":"YourOwnLongPassword!"}
```

Name: 1–80 nonblank characters. Email: a valid address up to 254 characters, normalized to lowercase. Password: 10–64 characters and no more than 72 UTF-8 bytes. Response: `201 {"id":"..."}`. Duplicate email: HTTP 409. Registration does not sign in automatically; the frontend follows with login.

## Login

Content-Type: `application/x-www-form-urlencoded`. Fields: `email`, `password`. Success: `200 {"ok":true}` with a session cookie. Wrong credentials: 401. The browser API client normalizes email before submitting.

## Send money

Required headers: `Content-Type: application/json`, the CSRF header, and `Idempotency-Key` (8–100 ASCII letters/digits/underscores/hyphens; a UUID works).

```json
{"recipientEmail":"maya@fincore.dev","amount":"125.50","note":"Lunch","category":"Food"}
```

The amount is expressed in rupees, up to 2 decimal places, between 0.01 and 1000000.00. Notes have at most 200 characters. Supported categories: Transfer, Shopping, Food, Travel, Bills, Income. The frontend uses Income only for incoming seed funds.

Response: transfer id, sourceId, targetId, amountCents, note, category, createdAt. A successful replay returns the same transfer id. Amount fields in responses are integer paise; divide by 100 for display only.

The server chooses the source from the session. It does not accept a source wallet or balance from the client. A recipient must have a registered account. Self-transfers are rejected.

## Errors

- 400: validation or malformed request.
- 401: session missing/expired or incorrect login.
- 403: missing/invalid CSRF token.
- 404: unknown recipient or inaccessible receipt.
- 409: insufficient funds, duplicate email, or a reused request key with a changed payload.

Application validation errors return a JSON `message`. Framework-level errors may use Spring's default error format. Unexpected internal errors are not exposed with database details.
