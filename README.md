# Enterprise Reconciliation System (ERS)

A modular reconciliation platform: data ingestion, transaction matching, reconciliation management (Bank & Cash / GL / Intercompany), exception triage, adjustment & journal posting, and compliance/governance — with JWT + RBAC auth, structured logging, and a responsive web UI.

## Stack

- **Backend**: Java 25, Spring Boot 3.3, Spring Security, Spring Data JPA, PostgreSQL, Flyway, Maven multi-module
- **Frontend**: React 19 + TypeScript, Vite, MUI, TanStack Query, React Router
- **Database**: any PostgreSQL 13+ server works — a local native install, a VPS install, or Docker if you prefer it. No Postgres-version-specific SQL is used (JSONB and Flyway migrations are portable), so nothing in the app needs to change based on how Postgres is hosted.

## Repository layout

```
backend/
  ers-common/          shared entities, DTOs, exceptions, enums
  ers-security/         auth, JWT, RBAC, admin user/role management
  ers-compliance/        audit trail, maker-checker approvals, period lock
  ers-ingestion/          data sources, file upload/parsing (CSV/JSON; XML stubbed)
  ers-matching/            match rules, deterministic matching engine
  ers-reconciliation/      templates, Bank & Cash reconciliation flow
  ers-exception/            break detection & triage workflow
  ers-adjustment/            journal entries, maker-checker posting, ERP stub
  ers-web/                   bootable Spring Boot app (migrations, config, dashboard API)
frontend/ers-ui/              React SPA
deploy/                       systemd unit, nginx config, DB init script, env template for a VPS deploy
docker-compose.yml            OPTIONAL alternative to a native Postgres install - not required
```

## Running locally (against your own PostgreSQL install)

The app just needs a database and role to exist; it doesn't care whether Postgres is native or containerized. Point it at whatever Postgres you already have running.

1. Create the role and database (see [deploy/db/init-db.sql](deploy/db/init-db.sql)):
   ```powershell
   & 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -f deploy\db\init-db.sql
   ```
   This creates role `ers` / password `ers` and database `ers`, matching the app's defaults (`localhost:5432`). Change the password in the script first if this box isn't purely local/throwaway.
2. Build and run the backend:
   ```bash
   cd backend
   mvn -DskipTests package
   java -jar ers-web/target/ers-web-0.1.0-SNAPSHOT.jar
   ```
   The app boots on `http://localhost:8080`, runs Flyway migrations, and seeds default roles/permissions plus an admin user (`admin` / `ChangeMe123!` by default — **change this in any real deployment** via `ERS_ADMIN_PASSWORD`). Swagger UI is at `/swagger-ui.html`.

   If your Postgres uses a different host/port/db/user/password, override via env vars: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.
3. Run the frontend:
   ```bash
   cd frontend/ers-ui
   npm install
   npm run dev
   ```
   Opens on `http://localhost:5173`, pointed at the backend via `VITE_API_BASE_URL` in `.env`.

## Deploying to a VPS

See the step-by-step Ubuntu 24.04 walkthrough below. In short: install Postgres/Java/Node/nginx natively, run `deploy/db/init-db.sql`, build both apps, run the backend as a systemd service (`deploy/systemd/ers-web.service` + `deploy/ers-web.env.example`), and serve the frontend + reverse-proxy `/api` with nginx (`deploy/nginx/ers.conf`).

## Operational features

- **Collapsible left nav**: toggle button in the top bar collapses the sidebar to an icon-only rail (desktop), preference persisted in `localStorage`. Mobile gets a separate overlay drawer.
- **Every data table**: row numbers, a "showing X–Y of Z" footer, page-size/page controls, and CSV/Excel/PDF export (exports the full filtered result set, not just the visible page) — see `frontend/ers-ui/src/components/DataTable.tsx`.
- **Sample data**: on first boot (empty DB), `SampleDataSeeder` creates demo users, data sources, ingests sample transactions, runs a real match + triggers a real reconciliation, resolves a break, and posts an adjustment through maker-checker — all through the actual application services, so you land on a populated dashboard with zero manual setup. Disable via `ERS_DEMO_ENABLED=false`.
- **LDAP/Active Directory login**: enabled by default against an embedded test directory (seeded from `backend/ers-security/src/main/resources/ldap/demo-users.ldif`), tried before local DB auth (configurable). Demo accounts below. Point at a real AD by setting `ers.security.ldap.embedded=false` plus `urls`/`base`/`manager-dn`/`manager-password`/`role-mappings` — no code changes.
- **Fine-grained RBAC**: write endpoints check specific permissions (`INGESTION_WRITE`, `MATCHING_CONFIGURE`, `MATCHING_RUN`, `EXCEPTION_TRIAGE`, `ADJUSTMENT_CREATE`, `ADJUSTMENT_APPROVE`, `ADMIN_USERS`) rather than hardcoded role lists, defined once in `DataSeeder`'s role→permission map. LDAP-mapped roles get their permissions from the same table, so both auth sources enforce identically.

### Demo accounts

| Username | Password | Source | Role |
|---|---|---|---|
| `admin` | `ChangeMe123!` | Local | ADMIN |
| `maker1` | `Maker@12345` | Local | RECON_MAKER |
| `checker1` | `Checker@12345` | Local | RECON_CHECKER |
| `compliance1` | `Compliance@12345` | Local | COMPLIANCE |
| `viewer1` | `Viewer@12345` | Local | VIEWER |
| `ldap.admin` / `ldap.maker` / `ldap.checker` / `ldap.compliance` / `ldap.viewer` | `ldap123` | Embedded LDAP | matching role |

## What's fully implemented vs. scaffolded

**Fully working, verified end-to-end**: auth (JWT + RBAC + login audit), CSV/JSON ingestion, deterministic rule-based matching, Bank & Cash reconciliation triggering, automatic break detection, exception triage (assign/escalate/resolve), maker-checker adjustment approval with auto-posting to a stub ERP client, period lock enforcement, immutable audit trail, dashboard aggregation.

**Scaffolded, follow-up needed**:
- XML ingestion parser (interface + registration in place, `UnsupportedOperationException` today)
- ML-based match scoring and exception prioritization (interfaces provided — `MatchScorer`, `ExceptionPrioritizer` — with rule-based default implementations only)
- General Ledger and Intercompany reconciliation comparison logic (CRUD scaffolding only; only Bank & Cash is wired to the matching engine)
- Real external connectors: SFTP polling, bank/POS/CRM APIs, actual Oracle Cloud EPM posting (currently a logging no-op stub — see `ErpPostingClient`)
- Response DTOs to replace direct entity serialization (`spring.jpa.open-in-view` is currently `true` as a stopgap — see the note in `application.yml`)
- Automated test suite and CI pipeline

For More Information
Please email us : info@bdtsolution.com
website: bdtsolution.com
