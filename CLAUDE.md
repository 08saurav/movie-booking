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

Segment 3 (Seat Inventory & Concurrency Core) complete: atomic conditional
UPDATE closes the TOCTOU race window entirely at the database level. Migration
`V4__segment3_concurrency.sql` adds `held_by` and `hold_expires_at` columns
to `show_seats`. `SeatHoldService` orchestrates the hold logic; `SeatHoldController`
exposes POST/DELETE endpoints. Holds are configurable (default 10 minutes) and
automatically swept every minute by `HoldExpirySweeperTask`. `GET /api/shows/{id}/seats`
now returns all seats with real-time status (AVAILABLE/HELD/BOOKED/CANCELLED).
`SeatHoldConcurrencyTest` verifies exactly 1 of N concurrent hold attempts
succeeds (HTTP 200) and N-1 fail with 409 Conflict. `mvn test` passes all 16
tests including the concurrency test on real PostgreSQL.

Segment 4 (Pricing, Discounts, Payment & Confirmation) complete: Migration
`V5__segment4_pricing_bookings.sql` adds `pricing_tiers`, `discount_codes`, and
`bookings` tables; also adds nullable `pricing_tier_id` FK to `shows`. A seeded
"Standard" tier (regular ₹150, premium ₹250, weekend ×1.25) is assigned to all
V3 shows so they are immediately bookable. `BookingService` implements the full
state machine: HELD→BOOKED→CONFIRMED/PAYMENT_FAILED. The seat transitions
HELD→BOOKED atomically before payment so the sweeper cannot expire it during
processing; on failure the seat is released to AVAILABLE. `PricingService`
computes base×weekendMultiplier×(1−discount). Discount code use-count increments
are serialized via a pessimistic write lock (`PESSIMISTIC_WRITE`). Idempotency
is enforced by a `UNIQUE(idempotency_key)` constraint; re-submitting the same
key returns the existing booking. `MockPaymentGatewayImpl` supports toggling
failure via `booking.payment.always-fail` or the `setAlwaysFail` method (for
tests without Spring context restarts). `BookingFlowTest` (7 tests) covers
happy path, idempotency, payment failure, discount codes, admin/customer list
views. `mvn test` passes all 23 tests on real PostgreSQL.

Segment 5 (Cancellation, Refunds & Async Notifications) complete: Migration
`V6__segment5_refunds_notifications.sql` adds `refund_policies` /
`refund_policy_tiers` tables and `refund_amount` / `cancelled_at` columns to
`bookings`. Shows carry an optional `refund_policy_id` FK; the default policy
(100% ≥24h, 50% 12–24h, 0% <12h) is seeded and assigned to all V3 shows.
`BookingService.cancel()` validates CONFIRMED status, computes refund by finding
the largest tier whose `hoursBeforeShow ≤ hoursUntilShow`, releases the seat to
AVAILABLE, and publishes `BookingCancelledEvent`. Customers cancel their own
bookings (`POST /api/bookings/{id}/cancel`); admins can cancel any booking
(`POST /api/admin/bookings/{id}/cancel`). Notifications fire via
`@TransactionalEventListener(AFTER_COMMIT) + @Async` on a dedicated
`notification-*` thread pool (`AsyncConfig`), so the HTTP response is returned
before the email fires. Events carry primitives only (no JPA entity refs) to
avoid LazyInitializationException on the async thread. `CancellationAndNotificationTest`
(5 tests) verifies refund calculation, double-cancel rejection, admin override,
and proves the listener runs on a background thread using a static
`LinkedBlockingQueue` (blocking `poll()` is deterministic regardless of Spring's
CGLIB proxy wrapping). `mvn test` passes all 28 tests on real PostgreSQL.
