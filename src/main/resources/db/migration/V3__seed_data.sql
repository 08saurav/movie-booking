-- Seed data for local development and integration tests, applied once to a
-- fresh database. Gives 2 cities, 2 theaters per city, 2 screens per theater,
-- a generated seat layout per screen (5 rows x 8 cols, back two rows
-- PREMIUM), 5 movies, and 2 shows per screen (16 shows total) with a full
-- AVAILABLE show_seat inventory -- enough to exercise every Segment 2
-- endpoint without any manual setup.
--
-- Seats, shows, and show_seats are generated with set-returning SQL rather
-- than hand-enumerated, both to keep this file maintainable and to mirror
-- how the application itself derives them (ScreenService / ShowService).

-- ---------------------------------------------------------------------
-- Cities
-- ---------------------------------------------------------------------
INSERT INTO cities (name, state, country) VALUES
    ('Bengaluru', 'Karnataka', 'India'),
    ('Mumbai', 'Maharashtra', 'India');

-- ---------------------------------------------------------------------
-- Theaters (2 per city)
-- ---------------------------------------------------------------------
INSERT INTO theaters (city_id, name, address) VALUES
    ((SELECT id FROM cities WHERE name = 'Bengaluru'), 'PVR Forum', 'Hosur Road, Koramangala, Bengaluru'),
    ((SELECT id FROM cities WHERE name = 'Bengaluru'), 'INOX Garuda Mall', 'Magrath Road, Bengaluru'),
    ((SELECT id FROM cities WHERE name = 'Mumbai'), 'PVR Phoenix Marketcity', 'Kurla, Mumbai'),
    ((SELECT id FROM cities WHERE name = 'Mumbai'), 'INOX R-City', 'Ghatkopar, Mumbai');

-- ---------------------------------------------------------------------
-- Screens (2 per theater, uniform 5 rows x 8 cols = 40 seats each)
-- ---------------------------------------------------------------------
INSERT INTO screens (theater_id, name, total_rows, total_cols)
SELECT t.id, sn.name, 5, 8
FROM theaters t
CROSS JOIN (VALUES ('Screen 1'), ('Screen 2')) AS sn (name);

-- ---------------------------------------------------------------------
-- Seats: generated per screen from its own total_rows/total_cols.
-- Row letters A.. ; the back two rows (closest to the unique constraint's
-- "last" rows) are PREMIUM, the rest REGULAR.
-- ---------------------------------------------------------------------
INSERT INTO seats (screen_id, row_label, seat_number, category)
SELECT s.id,
       chr(64 + r.rn),
       c.cn,
       CASE WHEN r.rn > (s.total_rows - 2) THEN 'PREMIUM' ELSE 'REGULAR' END
FROM screens s
         CROSS JOIN LATERAL generate_series(1, s.total_rows) AS r (rn)
         CROSS JOIN LATERAL generate_series(1, s.total_cols) AS c (cn);

-- ---------------------------------------------------------------------
-- Movies
-- ---------------------------------------------------------------------
INSERT INTO movies (title, duration_minutes, language, genre, rating, description) VALUES
    ('Inter-Galactic Heist', 142, 'English', 'Sci-Fi', 'UA', 'A ragtag crew of smugglers attempts the heist of the century.'),
    ('Monsoon Wedding Blues', 128, 'Hindi', 'Romance', 'U', 'Two families collide over a wedding nobody planned for.'),
    ('The Last Verdict', 135, 'English', 'Thriller', 'A', 'A retiring judge uncovers a conspiracy in her final case.'),
    ('Chasing Shadows', 118, 'Hindi', 'Action', 'UA', 'An undercover cop races to stop a heist before it starts.'),
    ('Laugh Riot', 105, 'English', 'Comedy', 'U', 'Three friends, one wedding, zero plans, total chaos.');

-- ---------------------------------------------------------------------
-- Shows: 2 time slots per screen (16 shows total), movie assignment cycled
-- across screens/slots for variety. Times are relative to now() so the seed
-- data is always "upcoming", regardless of when this migration runs.
-- ---------------------------------------------------------------------
WITH numbered_screens AS (
    SELECT id AS screen_id, row_number() OVER (ORDER BY id) AS rn
    FROM screens
),
numbered_movies AS (
    SELECT id AS movie_id, duration_minutes, row_number() OVER (ORDER BY id) AS rn, count(*) OVER () AS cnt
    FROM movies
),
show_slots (slot, day_offset, start_hour) AS (
    VALUES (1, 1, 18), (2, 3, 15)
)
INSERT INTO shows (movie_id, screen_id, start_time, end_time)
SELECT nm.movie_id,
       ns.screen_id,
       date_trunc('day', now()) + (ss.day_offset || ' days')::interval + (ss.start_hour || ' hours')::interval,
       date_trunc('day', now()) + (ss.day_offset || ' days')::interval + (ss.start_hour || ' hours')::interval
           + (nm.duration_minutes || ' minutes')::interval
FROM numbered_screens ns
         CROSS JOIN show_slots ss
         JOIN numbered_movies nm ON nm.rn = ((ns.rn + ss.slot - 2) % nm.cnt) + 1;

-- ---------------------------------------------------------------------
-- Show seats: one AVAILABLE row per (show, seat) on that show's screen.
-- ---------------------------------------------------------------------
INSERT INTO show_seats (show_id, seat_id, status)
SELECT sh.id, se.id, 'AVAILABLE'
FROM shows sh
         JOIN seats se ON se.screen_id = sh.screen_id;
