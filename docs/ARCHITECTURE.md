# Engineering decisions

## Transfer posting

1. Validate a positive amount, scale, category, destination, and request key.
2. Sort source and target wallet IDs and acquire `SELECT ... FOR UPDATE` locks in that order.
3. While the locks are held, check `(source_id, idempotency_key)` for a previous transfer.
4. An identical replay returns the original transfer. A replay with changed amount, recipient, note, or category returns HTTP 409.
5. Verify sufficient funds, insert the transfer, debit the sender, credit the recipient, insert two ledger entries, and append an audit event.
6. Commit all writes together. Any database exception rolls them back together.

Wallet balance is a transactionally maintained projection; ledger entries are the posting record. Balances are stored in integer paise, INR only. User wallets begin at zero and receive opening funds as a real simulated treasury transfer, so opening credit is traceable. The treasury starts with a fixed virtual reserve, so transfers—including onboarding grants—conserve total balance across all wallets.

## Concurrency

An outgoing transfer locks the source wallet before checking its balance, so two concurrent payments cannot both spend an unlocked stale balance. Locking both wallets in a global order prevents the usual A-to-B / B-to-A deadlock. A unique database key is a second layer of idempotency protection.

Identical requests must preserve their idempotency key. The confirmation screen keeps that key through retries; editing the request creates a new key. A dropped response after commit can be retried with the original key. Different keys represent distinct payment instructions.

The default isolation level is sufficient for the lock-based posting flow. The unit of consistency is the database transaction, not a distributed Redis lock. Caching balances or moving ledger posting across independent services would add consistency problems without a benefit for this project size.

## Database constraints

Wallet balance must be nonnegative. Transfer amount must be positive. Source and target must differ. A source/request-key pair is unique. A transfer has at most one entry per wallet. Foreign keys prevent orphan records.

Paired entries and zero-sum posting are enforced by the service transaction and verified by tests; there is no deferred SQL trigger guaranteeing the sum across ledger rows. Direct privileged database writes can still bypass the application's invariants. An advanced next step is a restricted posting procedure and database-enforced balanced journal batches.

## Authentication and authorization

Spring Security validates BCrypt hashes and stores identity in a server-side HTTP session. Same-origin clients use an HttpOnly cookie and request CSRF tokens. This keeps tokens out of localStorage. Login, logout, registration, and transfer calls require a CSRF token. Wallet ownership derives from the authenticated email, not a request parameter.

Receipt access requires membership in the transfer. The API supports exact recipient lookup by email, which reveals that a registered recipient exists; it does not provide a public user directory. Rate limiting should precede public deployment.

## Tradeoffs

- H2 enables easy local development; PostgreSQL is the Docker database. Passing H2 tests alone is not proof of PostgreSQL behavior. A separate Testcontainers test is included and runs in CI.
- Servlet sessions live in memory, so restarts sign users out and multiple instances require shared session storage or routing.
- Summary values cover all time and include opening funds. The UI labels them accordingly; it does not fabricate monthly trends.
- Search and CSV export operate on the loaded page, explicitly labeled. Global server-side search is a future enhancement.
- Seed transactions are timestamped when seeded; there is no fake historical data.
- A dedicated admin dashboard, fraud detector, notification pipeline, and production observability are future work.

## Useful interview demonstrations

Explain why BigDecimal becomes integer paise, what happens when a client retries after a timeout, why wallet lock ordering matters, how a failed ledger insert rolls back earlier balance updates, and how an authenticated user is prevented from selecting another sender's wallet. Run the concurrent spending and rollback tests to demonstrate behavior with evidence.
