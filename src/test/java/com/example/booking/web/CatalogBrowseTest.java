package com.example.booking.web;

import com.example.booking.AbstractIntegrationTest;
import com.example.booking.web.dto.CityResponse;
import com.example.booking.web.dto.ShowResponse;
import com.example.booking.web.dto.ShowSeatResponse;
import com.example.booking.web.dto.TheaterResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the customer browse endpoints against the data seeded by
 * V3__seed_data.sql. Assertions are framed as "contains at least" rather
 * than "contains exactly", since the same Testcontainers Postgres instance
 * is shared with CatalogAdminTest, which adds its own throwaway rows.
 */
class CatalogBrowseTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private TestRestTemplate asCustomer() {
        return rest.withBasicAuth("customer", "customer123");
    }

    @Test
    void listCities_includesSeedCities() {
        ResponseEntity<CityResponse[]> response = asCustomer().getForEntity("/api/cities", CityResponse[].class);
        List<CityResponse> cities = Arrays.asList(response.getBody());

        assertThat(cities).anySatisfy(c -> {
            assertThat(c.name()).isEqualTo("Bengaluru");
            assertThat(c.state()).isEqualTo("Karnataka");
        });
        assertThat(cities).anySatisfy(c -> assertThat(c.name()).isEqualTo("Mumbai"));
    }

    @Test
    void listTheatersInCity_returnsOnlyThatCitysTheaters() {
        Long bengaluruId = findSeedCityId("Bengaluru");

        ResponseEntity<TheaterResponse[]> response =
                asCustomer().getForEntity("/api/cities/" + bengaluruId + "/theaters", TheaterResponse[].class);
        List<TheaterResponse> theaters = Arrays.asList(response.getBody());

        assertThat(theaters).extracting(TheaterResponse::name).contains("PVR Forum", "INOX Garuda Mall");
        assertThat(theaters).allSatisfy(t -> assertThat(t.cityId()).isEqualTo(bengaluruId));
    }

    @Test
    void listUpcomingShowsForTheater_returnsFutureShowsSortedByStartTime() {
        Long forumTheaterId = findSeedTheaterId("PVR Forum");

        ResponseEntity<ShowResponse[]> response =
                asCustomer().getForEntity("/api/theaters/" + forumTheaterId + "/shows", ShowResponse[].class);
        List<ShowResponse> shows = Arrays.asList(response.getBody());

        assertThat(shows).isNotEmpty();
        assertThat(shows).allSatisfy(s -> assertThat(s.startTime()).isAfter(Instant.now()));
        assertThat(shows).isSortedAccordingTo((a, b) -> a.startTime().compareTo(b.startTime()));
    }

    @Test
    void listAvailableSeatsForShow_returnsFullScreenInventoryAllAvailable() {
        Long showId = jdbcTemplate.queryForObject("SELECT id FROM shows ORDER BY id LIMIT 1", Long.class);
        Long screenId = jdbcTemplate.queryForObject("SELECT screen_id FROM shows WHERE id = ?", Long.class, showId);
        int totalSeatsOnScreen = jdbcTemplate.queryForObject(
                "SELECT total_rows * total_cols FROM screens WHERE id = ?", Integer.class, screenId);

        ResponseEntity<ShowSeatResponse[]> response =
                asCustomer().getForEntity("/api/shows/" + showId + "/seats", ShowSeatResponse[].class);
        List<ShowSeatResponse> seats = Arrays.asList(response.getBody());

        assertThat(seats).hasSize(totalSeatsOnScreen);
        assertThat(seats).allSatisfy(s -> assertThat(s.status()).isEqualTo("AVAILABLE"));
    }

    @Test
    void listAvailableSeatsForShow_excludesSeatsThatAreNotAvailable() {
        // Seeded show_seats are all AVAILABLE in Segment 2 (no hold/book flow yet).
        // Flip one directly in the database to simulate a future-segment state and
        // prove the "available only" filter actually filters.
        Long showId = jdbcTemplate.queryForObject(
                "SELECT show_id FROM show_seats GROUP BY show_id HAVING count(*) > 1 LIMIT 1", Long.class);
        Long seatToBook = jdbcTemplate.queryForObject(
                "SELECT id FROM show_seats WHERE show_id = ? LIMIT 1", Long.class, showId);
        int countBefore = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM show_seats WHERE show_id = ? AND status = 'AVAILABLE'", Integer.class, showId);

        jdbcTemplate.update("UPDATE show_seats SET status = 'BOOKED' WHERE id = ?", seatToBook);
        try {
            ResponseEntity<ShowSeatResponse[]> response =
                    asCustomer().getForEntity("/api/shows/" + showId + "/seats", ShowSeatResponse[].class);
            List<ShowSeatResponse> seats = Arrays.asList(response.getBody());

            assertThat(seats).hasSize(countBefore - 1);
            assertThat(seats).noneMatch(s -> s.showSeatId().equals(seatToBook));
        } finally {
            jdbcTemplate.update("UPDATE show_seats SET status = 'AVAILABLE' WHERE id = ?", seatToBook);
        }
    }

    private Long findSeedCityId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM cities WHERE name = ?", Long.class, name);
    }

    private Long findSeedTheaterId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM theaters WHERE name = ?", Long.class, name);
    }
}
