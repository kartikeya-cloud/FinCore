# Development roadmap

## Implemented: core wallet MVP

Spring Boot API, React UI, register/login/logout, persistent local data, PostgreSQL Compose config, atomic virtual transfers, paired ledger entries, idempotency, ordered row locks, history, receipts, spending summaries, CSV export, database migrations, automated tests, and CI configuration.

## Next: deployment and operational readiness

1. Run the PostgreSQL integration test on a Docker-enabled machine and verify concurrent tests against PostgreSQL as well as H2.
2. Add frontend browser tests for login, registration, transfer confirmation, retries, receipt dialogs, keyboard navigation, and narrow mobile widths.
3. Add rate limits, password recovery, email verification, account lockouts, shared sessions, and secure deployment configuration.
4. Deploy the Spring Boot service and managed PostgreSQL behind HTTPS; configure the frontend's same-origin API proxy. Keep credentials outside Git.
5. Add metrics for rejected/posted transfers and latency, structured logs with trace IDs, backup and restore checks, and reconciliation jobs.

## Next: advanced portfolio features

1. Reconciliation report: compare each wallet projection with summed ledger entries and alert on a mismatch.
2. Refunds as compensating transfers, preserving original immutable posting records.
3. Admin investigation dashboard with explicit roles, scoped audit search, and sensitive-operation controls.
4. Global transaction search, date ranges, monthly trends from real records, and export jobs.
5. Load-test scripts with measured PostgreSQL throughput and p95 latency; publish actual measurements rather than invented numbers.

## Optional AI extension

If you want to continue toward your AI goal, add explainable suspicious-transfer detection using synthetic labeled data. First build deterministic signals (unusual amount, rapid transfer frequency, new recipient); then compare a model against that baseline. Keep it advisory and measure precision/recall on a held-out dataset. A spending assistant could answer questions from the authenticated user's transaction summaries with grounded data and citations. Neither feature should be described as implemented yet.

## Resume wording after you run and understand the project

“Built a Java/Spring Boot and React wallet simulation with atomic transfers, paired ledger entries, ordered row locks, idempotent payment requests, and automated concurrency and security tests.”

Add PostgreSQL deployment, measured load-test numbers, or AI features only after you have personally verified those results. Be ready to explain and modify the transfer code in an interview.
