# CLAUDE.md

Guidance and working agreement for AI-assisted development of this project.
This file is both a working document and a required submission artifact (it
records how AI was used during development).

## Project

Movie Ticket Booking System — a Spring Boot backend. See `README.md` for the
product overview and the segment roadmap.

## How AI is being used

Development proceeds in small, independently reviewable **segments**. For each
segment the workflow is:

1. AI proposes/implements one segment only (code + tests + docs).
2. The developer reviews and tests it (compiles, runs, exercises endpoints in
   Swagger, runs `mvn test`).
3. On approval, AI proceeds to the next segment. Review feedback is folded in
   before advancing.

This keeps every change small enough to fully understand and verify, and yields
a commit-per-segment history. The developer owns all design decisions and
reviews every line; AI accelerates scaffolding and boilerplate. The concurrency
core (Segment 3) is reviewed with particular care and understood in depth, not
accepted as a black box.

## Conventions

- **Architecture**: layered — `web` (controllers/DTOs) → `service` (business
  logic, transactions) → `repository` (Spring Data) → `domain` (JPA entities).
  Controllers stay thin; business rules live in services.
- **Packages**: `com.example.booking.<layer>` (`config`, `web`, `service`,
  `repository`, `domain`, ...).
- **Schema**: owned by Flyway migrations under
  `src/main/resources/db/migration` (`V<n>__description.sql`). Never rely on
  Hibernate auto-DDL; `ddl-auto` stays `validate`.
- **API**: REST under `/api`. Admin-only endpoints live under `/api/admin/**`.
- **Validation**: Bean Validation on request DTOs; a global exception handler
  returns consistent, structured error responses (added in a later segment).
- **Tests**: integration tests use Testcontainers Postgres and end in `Test`.
  Core flows — especially concurrent seat booking — must be covered.
- **Style**: clear names over cleverness; Javadoc on non-obvious classes and on
  anything explaining a design decision.

## Constraints (out of scope — do not build)

UI/frontend; application containerization, deployment, CI/CD; distributed
systems/microservices; advanced auth (OAuth/SSO/MFA); production-grade
observability. Keep auth to HTTP Basic with role-based access.

## Current status

Segment 1 (Foundation) complete: runnable secured skeleton, Postgres + Flyway,
Swagger UI, HTTP Basic with `admin`/`customer` roles, ping smoke-test endpoints,
and an integration test verifying RBAC.

Segment 2 (Catalog & Browse) complete: `City`/`Theater`/`Screen`/`Seat`/`Movie`/
`Show`/`ShowSeat` domain model (`V2__catalog_schema.sql`), seed data
(`V3__seed_data.sql`: 2 cities, 4 theaters, 8 screens with generated seat
layouts, 5 movies, 16 shows with full seat inventory). Admin CRUD for City/
Theater/Screen/Movie/Show (seats and show-seats are generated automatically,
never created directly); customer browse endpoints
(`/api/cities`, `/api/cities/{id}/theaters`, `/api/theaters/{id}/shows`,
`/api/shows/{id}/seats`). `GlobalExceptionHandler` returns a consistent
`{timestamp, status, error, message, path}` shape for not-found/validation/
data-integrity/unexpected errors. `mvn test` passes (15 tests, real Postgres
via Testcontainers using the "singleton container" pattern — see
`AbstractIntegrationTest`'s Javadoc for why the naive `@Testcontainers`/
`@Container` combo broke across multiple test classes).
