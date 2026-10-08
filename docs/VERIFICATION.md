# Verification — 8 October 2026

| Check | Result |
| --- | --- |
| Spring Boot compile and executable JAR packaging | Passed |
| Default backend suite, H2 PostgreSQL compatibility mode | 16 tests passed; 0 failures, 0 errors, 0 skips |
| API security and validation suite | 9 tests passed |
| Ledger correctness and concurrency suite | 7 tests passed |
| React/Vite production build | Passed |
| Packaged JAR serves dashboard HTML and built JS/CSS | Passed through HTTP |
| Live session login, transfer, identical retry, balanced receipt, history, logout | Passed through HTTP against seeded H2 instance |
| PostgreSQL Testcontainers test | Included; not run locally (Docker unavailable) |
| Docker Compose startup | Configuration included; not run locally (Docker unavailable) |
| Browser UI, keyboard, and mobile visual testing | Not performed |
| GitHub Actions run | Workflow included; not run on GitHub |
| Live cloud deployment | Not performed |

Local runtime: OpenJDK 17.0.20, Maven 3.9.11, Node 24.19.0. Docker images target Java 21, Node 24, and PostgreSQL 17. Dependencies were resolved successfully and the backend and frontend were built in this environment.

Tests cover: balance debit/credit and conservation, two zero-sum entries, idempotent replay, changed-payload rejection, invalid/insufficient funds, 20 concurrent spend requests, 16 concurrent identical requests, 20 opposite-direction requests, forced ledger constraint failure and rollback, authentication, CSRF, session-derived sender, amount scale, pagination validation, logout, and receipt participant authorization.

The tests run against an actual Spring context and an H2 database, without mocking the transfer service. Mockito uses its subclass mock maker because this environment disallows dynamic Java agent attachment. This setting affects tests only.

The HTTP smoke check posts a virtual ₹1 transfer and verifies its duplicate replay updates the sender balance only once. Both live checks used disposable in-memory databases; no test accounts or passwords were copied into the deliverable as persisted database files.

The release JAR bundles the latest frontend build. The ZIP excludes node_modules, backend build directories, local databases, logs, and environment secrets. Read README.md for setup and docs/ROADMAP.md for remaining milestones.
