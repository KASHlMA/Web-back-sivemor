# SIVEMOR Platform

This workspace is the `sivemor-platform` repository. It contains:

- `backend/`: Spring Boot Kotlin API with RBAC, Flyway, MySQL, audit logging, admin CRUD, technician inspection draft flows, and failure-focused reporting.
- `web-admin/`: React + TypeScript admin panel with login, dashboard, CRUD screens, and report filters.
- `docker-compose.yml`: local platform stack for MySQL, backend, and web admin.
- `contracts/openapi.yaml`: shared API contract stub for external clients, including the separate Android repository.

## Repository layout

```text
.
├── backend
├── contracts
├── docker-compose.yml
└── web-admin
```

## Local startup

1. Copy the environment template:

   ```bash
   cp .env.example .env
   ```

2. Start the production-like platform stack:

   ```bash
   docker compose up --build
   ```

   This flow builds immutable images for the backend and web admin. It is useful when you want to test the stack close to deployment behavior.
   It should be the command you use when you want Docker to serve the final redesigned frontend build at `http://localhost:3000`.
   The backend now applies schema changes through Flyway. If you have an older local MySQL volume created before this change, reset it once with:

   ```bash
   docker compose down -v
   ```

3. Start the live-reload development stack:

   ```bash
   docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
   ```

   This flow keeps MySQL the same, but runs:
   - the backend from source with Gradle `bootRun --continuous`
   - the web admin with Vite dev server and bind-mounted source

   In this mode, frontend edits should refresh automatically and backend code changes should trigger rebuild/restart behavior without manually stopping containers.
   On the first backend startup, the container may remain in `health: starting` for a while because Gradle compiles and boots the app from source before the TCP healthcheck can pass.
   This is the command you should use when you want to see the redesigned frontend and keep hot reload while you work.

4. Open the admin UI:

   - Web admin: `http://localhost:3000`
   - Backend API: `http://localhost:8081/api/v1`
   - Backend health: `http://localhost:8081/actuator/health`
   - Swagger UI: `http://localhost:8081/swagger-ui.html`

5. To call secured endpoints from Swagger UI:

   - Authenticate against `POST /api/v1/auth/login`
   - Copy the returned `accessToken`
   - Paste it into Swagger UI via the `Authorize` button as a Bearer token

## Seed users

- Administrator
  - username: `admin`
  - password: `Admin123!`
- Technician
  - username: `tecnico1`
  - password: `Tecnico123!`

## Notes

- The Android app lives in a separate repository and should target the backend through `http://10.0.2.2:8080/api/v1` when running in the Android emulator.
- In Docker, Flyway is the source of truth for schema changes. Every new entity or table change must include a migration.
- Technicians are restricted to draft capture flows only. The mobile API does not expose final report browsing.
- Evidence is stored in MySQL as binary data with metadata and checksum tracking.
- In live-reload Docker mode, dependency changes may still require container recreation or a fresh `docker compose ... up --build`.
