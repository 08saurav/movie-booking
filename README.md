# Movie Ticket Booking System

A backend for a movie ticket booking system: multiple cities, theaters, screens,
and shows, with seat-level booking, time-bound seat holds, pricing tiers and
discounts, payment, confirmation, and policy-based refunds. Built with Spring Boot.

> This README reflects the completed system through **Segment 6 (Hardening & Polish)**.

## Tech stack

| Concern              | Choice                              | Why |
|----------------------|-------------------------------------|-----|
| Language / runtime   | Java 21                             | LTS, modern language features (records, pattern matching). |
| Framework            | Spring Boot 3.3.x                   | Required by the assignment; mature ecosystem for REST, security, persistence. |
| Database             | PostgreSQL                          | Real row-level locking semantics, needed for correct seat-booking concurrency (Segment 3). |
| Persistence          | Spring Data JPA / Hibernate         | Standard, productive data access. |
| Migrations           | Flyway                              | Versioned, reviewable schema; the database is the source of truth, not `ddl-auto`. |
| API docs / manual QA | springdoc-openapi (Swagger UI)      | Live, interactive endpoint testing during review. |
| Security             | Spring Security, HTTP Basic         | Role-based access for `admin` / `customer`. Advanced auth (OAuth/SSO/MFA) is out of scope per the brief. |
| Tests                | JUnit 5, Spring Boot Test, Testcontainers | Integration tests run against a real Postgres for fidelity. |

## Prerequisites

- JDK 21
- Maven 3.9+
- A running PostgreSQL with a database named `movie_booking`
- Docker (only for running the test suite, which uses Testcontainers)

A quick way to get a local Postgres for development (this runs the *database* in a
container for convenience — the application itself is not containerized, in line
with the out-of-scope list):

```bash
docker run --name movie-booking-db -e POSTGRES_DB=movie_booking \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 -d postgres:16-alpine
```

Connection settings can be overridden with the `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD` environment variables (see `application.yml`).

## Run

```bash
mvn spring-boot:run
```

- API base path: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Demo credentials (Segment 1, in-memory)

| Username   | Password      | Role     |
|------------|---------------|----------|
| `admin`    | `admin123`    | ADMIN    |
| `customer` | `customer123` | CUSTOMER |

These are seeded in memory for development and review. They will move into the
database when user-owned data (bookings) is introduced in a later segment.

### Try it

In Swagger UI, click **Authorize**, enter one of the credential pairs above, then:

- `GET /api/ping` — succeeds for any authenticated user; returns 401 when anonymous.
- `GET /api/admin/ping` — succeeds only for `admin`; returns 403 for `customer`.

#### Walk through the full booking flow

1. **Browse** as `customer`: `GET /api/cities` → city id → `GET /api/cities/{id}/theaters`
   → theater id → `GET /api/theaters/{id}/shows` → note the `showId` and the `pricingTierId`
   → `GET /api/shows/{id}/seats` → pick an AVAILABLE seat, note its `showSeatId`.
2. **Hold** the seat: `POST /api/shows/{showId}/seats/{showSeatId}/hold` (as `customer`).
   Returns the hold expiry time (10 minutes by default).
3. **Book** it: `POST /api/bookings` with header `X-Idempotency-Key: <any UUID>` and body
   `{ "showId": ..., "showSeatId": ..., "discountCode": null }`.
   Returns a booking with `status: CONFIRMED` and a `finalPrice`.
4. **Idempotency**: re-submit the same `X-Idempotency-Key` — the same booking is returned
   without reprocessing or re-charging.
5. **View history**: `GET /api/bookings` (as `customer`) — your bookings, newest first.
6. **Admin view**: `GET /api/admin/bookings?page=0&size=20` — paginated list of all bookings.
7. **Pricing tiers**: `GET /api/admin/pricing-tiers` — lists the seeded "Standard" tier.
   `POST /api/admin/pricing-tiers` — create custom tiers.
8. **Discount codes**: `POST /api/admin/discount-codes` with `discountType: PERCENTAGE` and
   `value: 10` — creates a 10% off code. Use `"discountCode": "<code>"` in step 3 to apply it.
9. **Cancel**: `POST /api/bookings/{id}/cancel` (as `customer`) — cancels a CONFIRMED booking.
   Returns the updated booking with `status: CANCELLED`, `cancelledAt`, and `refundAmount`.
   Admins can cancel any booking via `POST /api/admin/bookings/{id}/cancel`.

##### Payment failure path

Set `booking.payment.always-fail: true` in `application.yml` and restart. The booking will
return `status: PAYMENT_FAILED` and the seat will revert to `AVAILABLE`. (Reset to `false`
for normal operation.)

Alternatively, any booking whose `finalPrice` is exactly `0.01` triggers a failure without
restarting, useful for targeted debugging.

#### Walk through the catalog (Segment 2)

The database is seeded on first startup (see [Seed data](#seed-data)), so you can
browse immediately without creating anything:

1. **Browse as `customer`**: `GET /api/cities` → pick a city id → `GET /api/cities/{id}/theaters`
   → pick a theater id → `GET /api/theaters/{id}/shows` → pick a show id →
   `GET /api/shows/{id}/seats` (only `AVAILABLE` seats are returned).
2. **Manage the catalog as `admin`**: under the `Admin: *` tags, try
   `POST /api/admin/cities`, then `POST /api/admin/theaters` with that city's id,
   then `POST /api/admin/screens` with that theater's id, `totalRows`/`totalCols`,
   and optionally `premiumRows` (e.g. `["D", "E"]`) — the seat layout is generated
   immediately; confirm it with `GET /api/admin/screens/{id}/seats`.
3. **Schedule a show**: `POST /api/admin/movies`, then `POST /api/admin/shows`
   with that movie's id, the screen's id, and a future `startTime` — this
   generates one `AVAILABLE` show-seat row per seat on that screen. Confirm via
   `GET /api/shows/{id}/seats` (as `customer`) or `GET /api/admin/shows/{id}`.
4. **See the delete-protection design**: try `DELETE /api/admin/movies/{id}` or
   `DELETE /api/admin/screens/{id}` for a movie/screen that has a show scheduled
   — both return `409 Conflict`. Delete the show first, then the delete
   succeeds.
5. **See validation and error handling**: `POST /api/admin/cities` with an empty
   `name`, or `GET /api/admin/cities/999999`, to see the consistent error shape
   (below).

#### Error response shape

Every error response (validation failures, not-found, conflicts, unexpected
errors) has this shape:

```json
{
  "timestamp": "2025-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "name: must not be blank",
  "path": "/api/admin/cities"
}
```

> Known gap: Spring Security's own 401/403 responses (wrong/missing
> credentials, insufficient role) are emitted by Security's filter chain
> *before* the request reaches a controller, so they don't yet go through
> `GlobalExceptionHandler` and don't have this shape. Revisiting this is a
> Segment 6 (Hardening) candidate if needed.

## Domain model

```
City 1──* Theater 1──* Screen 1──* Seat
Movie 1──* Show *──1 Screen
Show 1──* ShowSeat *──1 Seat
```

- **City → Theater → Screen → Seat** is a pure ownership hierarchy. A screen's
  seats are *generated*, not created directly: given `totalRows`/`totalCols`
  (and optionally which row letters are `PREMIUM`), `ScreenService` creates one
  `Seat` row per (row, column) at screen-creation time. There is no "create a
  seat" endpoint.
- **Movie → Show** schedules a movie on a screen at a time; `endTime` is
  derived from the movie's `durationMinutes`.
- **Show → ShowSeat** is the per-show seat inventory: creating a show
  generates one `ShowSeat` row per seat on its screen, all `AVAILABLE`. This
  is the table Segment 3's atomic hold mechanism operates on.
- Every parent/child relationship above is modeled as a unidirectional
  `@ManyToOne` only — there are no inverse `@OneToMany` collections (e.g. no
  `City.theaters`). Child lists are fetched through repository queries
  (`findByCityId`, etc.) instead of navigating bidirectional JPA associations,
  which sidesteps the N+1/stale-collection issues those associations invite.

### Delete semantics

Deletes are deliberately asymmetric, encoded as FK actions in
`V2__catalog_schema.sql`:

| Relationship | On delete | Why |
|---|---|---|
| City → Theater, Theater → Screen, Screen → Seat | `CASCADE` | Pure structural ownership — removing a city should remove everything beneath it. |
| Movie → Show, Screen → Show | `RESTRICT` | A movie or screen with scheduled shows can't be deleted out from under them. |
| Show → ShowSeat | `CASCADE` | Removing a show removes its per-show inventory. |
| Seat → ShowSeat | `RESTRICT` | A seat that has ever been part of a show can't be deleted. |

The `Seat → ShowSeat` restriction is the interesting one: combined with the
cascade chain above, it means deleting a city/theater/screen transitively
**fails** (as a single FK violation, surfaced as `409 Conflict` by
`GlobalExceptionHandler`) as soon as any show has ever been scheduled on one
of its screens. You can't tear down the catalog's structural skeleton out
from under historical show data — you have to delete the show(s) first. This
is exercised end-to-end in `CatalogAdminTest`.

### Seed data

`V3__seed_data.sql` seeds 2 cities, 2 theaters per city, 2 screens per theater
(5 rows × 8 cols each, back two rows `PREMIUM`), 5 movies, and 2 shows per
screen (16 shows total) with a full seat inventory — enough to exercise every
endpoint without manual setup. Seats, shows, and show-seats are generated with
set-returning SQL (`generate_series`, lateral joins) rather than hand-enumerated,
both to keep the file maintainable and because hand-writing ~1,000 rows would
be its own source of bugs.

## Catalog API (Segment 2)

| Endpoint | Role | Notes |
|---|---|---|
| `POST/PUT/DELETE/GET /api/admin/cities[/{id}]` | ADMIN | |
| `POST/PUT/DELETE/GET /api/admin/theaters[/{id}]` | ADMIN | `GET` supports `?cityId=` filter |
| `POST/PUT/DELETE/GET /api/admin/screens[/{id}]` | ADMIN | `GET` supports `?theaterId=` filter; `PUT` renames only (layout is fixed at creation) |
| `GET /api/admin/screens/{id}/seats` | ADMIN | Verifies the generated seat layout |
| `POST/PUT/DELETE/GET /api/admin/movies[/{id}]` | ADMIN | |
| `POST/PUT/DELETE/GET /api/admin/shows[/{id}]` | ADMIN | `PUT` reschedules (`startTime`) only |
| `GET /api/cities` | any authenticated user | |
| `GET /api/cities/{id}/theaters` | any authenticated user | |
| `GET /api/theaters/{id}/shows` | any authenticated user | Upcoming shows only, soonest first |
| `GET /api/shows/{id}/seats` | any authenticated user | All seats with real-time status (Segment 3) |

## Seat holds API (Segment 3)

| Endpoint | Role | Notes |
|---|---|---|
| `POST /api/shows/{showId}/seats/{seatId}/hold` | customer | Atomic conditional UPDATE prevents double-booking. Returns 200 with hold details (expiry time), or 409 if unavailable. |
| `DELETE /api/shows/{showId}/seats/{seatId}/hold` | customer | Release a hold early. Returns 204 on success. |

Hold TTL is configurable via `booking.seat-hold-ttl-minutes` (default 10 minutes) in `application.yml`. Expired holds are auto-swept every minute.

## Booking & pricing API (Segment 4)

| Endpoint | Role | Notes |
|---|---|---|
| `POST /api/bookings` | customer | Header `X-Idempotency-Key` required. Requires a HELD seat for this customer. Returns CONFIRMED or PAYMENT_FAILED. |
| `GET /api/bookings` | customer | Booking history, newest first. |
| `GET /api/bookings/{id}` | customer | Single booking; 404 if it belongs to someone else. |
| `POST /api/admin/pricing-tiers` | admin | Create a pricing tier (regularPrice, premiumPrice, weekendMultiplier ≥ 1.00). |
| `GET /api/admin/pricing-tiers` | admin | List all tiers. |
| `POST /api/admin/discount-codes` | admin | Create a discount code (PERCENTAGE or FLAT, with optional maxUses and validity window). |
| `GET /api/admin/discount-codes` | admin | List all codes. |
| `GET /api/admin/bookings` | admin | Paginated list of all bookings (`?page=0&size=20&sort=createdAt,desc`). |

## Cancellation & refunds API (Segment 5)

| Endpoint | Role | Notes |
|---|---|---|
| `POST /api/bookings/{id}/cancel` | customer | Cancel own CONFIRMED booking. Refund computed by show's refund policy. |
| `POST /api/admin/bookings/{id}/cancel` | admin | Cancel any CONFIRMED booking regardless of owner. |
| `POST /api/admin/refund-policies` | admin | Create a refund policy with tiers (hoursBeforeShow → refundPercent). |
| `GET /api/admin/refund-policies` | admin | List all refund policies. |
| `PUT /api/admin/refund-policies/{id}` | admin | Update a refund policy's tiers. |

### Refund policy model

Each `RefundPolicy` has a list of tiers, each with `hoursBeforeShow` and `refundPercent`. The system finds the tier with the **largest** `hoursBeforeShow` that is still ≤ hours until show start, and applies that tier's refund percentage. Example for the seeded policy:

| Hours before show | Refund |
|---|---|
| ≥ 24 h | 100% |
| ≥ 12 h | 50% |
| < 12 h | 0% |

Shows carry an optional `refundPolicyId`; if not set, the default policy (marked `isDefault = true`) is used. V6 seeds one default policy and assigns it to all V3 shows.

### Async notifications

Booking confirmation and cancellation trigger email notifications via `@TransactionalEventListener(AFTER_COMMIT) + @Async`. The HTTP response is returned before the notification fires. The notification thread pool is named `notification-*` and is configurable via `AsyncConfig`.

### Pricing model

```
finalPrice = basePrice × weekendMultiplier (if Sat/Sun UTC) × (1 − discountFraction)
```

- `basePrice` = `pricingTier.regularPrice` or `pricingTier.premiumPrice` based on the seat's category.
- Weekend is determined by the show's `startTime` in UTC.
- `FLAT` discounts are capped at `basePrice` (no negative totals).
- A show without a pricing tier returns 400 if booking is attempted.

### Booking state machine

```
HELD seat → POST /api/bookings → PAYMENT_PENDING (seat transitions to BOOKED)
  → payment success → CONFIRMED (seat stays BOOKED)
  → payment failure → PAYMENT_FAILED (seat released back to AVAILABLE)
```

Moving the seat to `BOOKED` before calling the payment gateway prevents the hold-expiry sweeper from reclaiming the seat during payment processing.

### Idempotency

`X-Idempotency-Key` is required on `POST /api/bookings`. If a booking with that key already exists, it is returned immediately without re-processing. Keys should be unique per booking attempt (UUID v4 recommended). The `UNIQUE` constraint on `bookings.idempotency_key` also prevents duplicate processing if two requests with the same key race to the database.

### Double-booking backstop

Two layers prevent double-booking:
1. **Segment 3**: atomic conditional UPDATE closes the TOCTOU window on the hold.
2. **Segment 4**: `UNIQUE(show_seat_id)` on the `bookings` table — even if the application logic had a flaw, the database would reject a second confirmed booking for the same show-seat.

## Test

```bash
mvn test
```

Integration tests boot a real PostgreSQL via Testcontainers, so Docker must be
running. Testcontainers is a *testing* dependency, distinct from the
application containerization/deployment that the brief lists as out of scope.

## Assumptions (running log)

1. `admin` and `customer` are the only two roles. Authentication is HTTP Basic;
   anything beyond that (OAuth, SSO, MFA) is explicitly out of scope.
2. The application runs as a single instance. Seat-booking correctness is handled
   at the database level (Segment 3); the multi-instance / distributed case is
   discussed but not implemented (out of scope).
3. PostgreSQL is the target database in all environments, including tests.
4. The schema is owned by Flyway; `spring.jpa.hibernate.ddl-auto` is set to
   `validate` so the app never silently mutates the schema.
5. Row labels are single letters (`A`–`Z`), capping a screen at 26 rows. Ample
   for this assignment's scale; multi-letter labels (`AA`, `AB`, ...) aren't
   implemented.
6. A screen's `totalRows`/`totalCols` and a show's `movie`/`screen` are fixed
   at creation and not editable via `PUT` — both would desync already-generated
   `Seat`/`ShowSeat` rows. `PUT /api/admin/screens/{id}` only renames; `PUT
   /api/admin/shows/{id}` only reschedules (`startTime`, with `endTime`
   recomputed). Re-laying-out a screen or moving a show to a different
   movie/screen means deleting and recreating it.
7. "Not found" is one exception type (`ResourceNotFoundException` → 404)
   whether the missing id came from the URL path or from a reference inside a
   request body (e.g. an unknown `cityId` when creating a theater). From the
   caller's perspective both are "the id you gave me doesn't exist."
8. The unique constraint on `shows(screen_id, start_time)` only prevents
   scheduling two shows on the same screen at the exact same instant; it does
   not detect general time-range overlap (a show starting mid-way through
   another). Full overlap validation is out of scope.
9. Browse endpoints (`/api/cities`, `/api/cities/{id}/theaters`, etc.) require
   only authentication, not `ROLE_ADMIN` — both `admin` and `customer` can
   browse, per `SecurityConfig`'s existing "admin-only under `/api/admin/**`,
   authenticated everywhere else" rule.
10. Seat holds are time-bound and automatically expired: an admin-configurable
    TTL (default 10 minutes) governs how long a customer can hold a seat before
    booking or releasing it. Expired holds are swept back to `AVAILABLE` every
    minute. Lazy expiry in the hold UPDATE WHERE clause ensures correctness
    (no concurrent thread can acquire an expired hold); the sweeper handles
    clean state for accurate reporting.
11. A show's `pricingTier` is a nullable FK. A show created without one can
    still be scheduled and browsed, but a booking attempt returns 400 with a
    clear message. The V5 migration seeds a "Standard" tier and assigns it to
    all V3 shows so the demo data is immediately bookable.
12. `DiscountCode.maxUses = null` means unlimited use. The `current_uses` count
    is incremented inside the booking transaction under a pessimistic write lock
    so concurrent bookings can't both read `0` and both claim the last use of a
    single-use code.
13. The payment gateway is a mock. No real charges are made. Mock failure can
    be triggered by setting `booking.payment.always-fail=true` or by booking a
    seat whose computed `finalPrice` is exactly `0.01`. The gateway abstraction
    (`PaymentGateway` interface) allows a real gateway to be substituted without
    changing booking logic.

## Design decisions

1. **Generated seats/show-seats over manual CRUD.** Seats are never created
   directly — `ScreenService` generates them from a screen's `totalRows` ×
   `totalCols` (plus an optional `premiumRows` set) at creation time, the same
   way `ShowService` generates one `ShowSeat` per seat when a show is created.
   This matches how the data is actually shaped (every seat must belong to a
   well-formed grid; every show must have a complete inventory) and removes an
   entire class of "admin forgot to add seat 37" bugs.
2. **Unidirectional `@ManyToOne` only, no inverse collections.** None of
   `City`, `Theater`, `Screen`, `Show` expose a `@OneToMany` back-reference.
   Child lists are fetched via repository queries instead of navigating
   bidirectional JPA associations — avoids the classic N+1 and stale-collection
   pitfalls those associations invite, at the cost of an extra query when you
   need both directions (a cost we're happy to pay here).
3. **Delete cascade/restrict as a deliberate, asymmetric design**, not a
   default. See [Delete semantics](#delete-semantics) above: pure ownership
   chains cascade, but anything with historical/scheduled data attached
   (`Movie`, `Screen`, transitively `Theater`/`City` once a show exists)
   blocks deletion with a `409`. This was chosen over cascading everything
   (which would silently destroy show history) or restricting everything
   (which would make tearing down test/seed data tedious).
4. **Atomic conditional UPDATE for race-free seat holds (Segment 3).** The hold
   operation uses a single `UPDATE` statement with a `WHERE` clause that checks
   availability: `UPDATE show_seats SET status='HELD', held_by=:userId, hold_expires_at=:expiresAt
   WHERE id=:seatId AND (status='AVAILABLE' OR (status='HELD' AND hold_expires_at < now()))`.
   The database serializes concurrent attempts at the WHERE clause level, closing
   the TOCTOU (time-of-check to time-of-use) window entirely. No application-level
   locking needed. Comparison of three approaches:
   - **Pessimistic locking** (`SELECT FOR UPDATE`): acquires an exclusive lock,
     blocking other readers until commit. Works but causes contention under load.
   - **Optimistic locking** (`@Version`): assumes collisions are rare; aborts if
     the entity changed since load. Simple but requires retry logic at the caller.
   - **Atomic conditional UPDATE** (chosen): the WHERE clause is the availability
     check; the database guarantees at most one thread changes it. No application
     retries, no locks held across requests, and full visibility (409 vs 200 tells
     you immediately if you lost). Fastest, clearest, and scales to high concurrency.

## Out of scope

Per the assignment brief, the following are intentionally not built:
UI / frontend; application containerization, deployment, or CI/CD; distributed
systems / microservices; advanced authentication (OAuth, SSO, MFA);
production-grade observability/monitoring/alerting.

## Build roadmap

The system is built in reviewable segments:

1. **Foundation** — runnable, secured, Swagger-visible skeleton with DB + Flyway. _(done)_
2. **Catalog & browse** — domain model, admin CRUD, customer browse, seed data. _(done)_
3. **Seat inventory & concurrency core** — atomic seat holds, auto-expiry, the double-booking test. _(done)_
4. **Pricing, discounts, payment, confirmation** — booking state machine end to end. _(done)_
5. **Cancellation, refunds, notifications** — policy-based refunds, async notifications. _(done)_
6. **Hardening & polish** — `@ApiResponse` Swagger docs, `@EnableSpringDataWebSupport`, full E2E integration test, README finalization. _(done)_
