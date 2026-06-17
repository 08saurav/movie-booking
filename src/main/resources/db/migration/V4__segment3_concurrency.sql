-- Segment 3: seat hold tracking and atomic concurrency control.
--
-- Adds the hold-tracking columns needed by the atomic conditional UPDATE that
-- makes seat holds race-free. The UPDATE itself lives in ShowSeatRepository
-- as a @Modifying @Query; the database serializes concurrent attempts.

ALTER TABLE show_seats
    ADD COLUMN held_by VARCHAR(100),
    ADD COLUMN hold_expires_at TIMESTAMPTZ;

-- Support the hold-expiry sweep (Segment 3) and lazy expiry in the UPDATE WHERE clause.
-- This composite index allows efficient queries like:
--   "find all HELD seats on show X that expired before now" (for the scheduled sweeper)
--   "is this seat HELD and not expired?" (checked inline in the conditional UPDATE)
CREATE INDEX idx_show_seats_hold_expiry
    ON show_seats (show_id, status, hold_expires_at)
    WHERE status = 'HELD';
