package com.example.booking.web;

import com.example.booking.AbstractIntegrationTest;
import com.example.booking.exception.ErrorResponse;
import com.example.booking.web.dto.CityRequest;
import com.example.booking.web.dto.CityResponse;
import com.example.booking.web.dto.MovieRequest;
import com.example.booking.web.dto.MovieResponse;
import com.example.booking.web.dto.ScreenRequest;
import com.example.booking.web.dto.ScreenResponse;
import com.example.booking.web.dto.SeatResponse;
import com.example.booking.web.dto.ShowRequest;
import com.example.booking.web.dto.ShowResponse;
import com.example.booking.web.dto.ShowSeatResponse;
import com.example.booking.web.dto.TheaterRequest;
import com.example.booking.web.dto.TheaterResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers admin CRUD for the full catalog hierarchy, seat/show_seat
 * auto-generation, the delete cascade/restrict design, and validation/role
 * enforcement. Each test creates its own throwaway data rather than
 * depending on the seed data's exact shape, since the Testcontainers
 * Postgres instance (and therefore the seeded rows) is shared across all
 * integration test classes in this run.
 */
class CatalogAdminTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    private TestRestTemplate asAdmin() {
        return rest.withBasicAuth("admin", "admin123");
    }

    private TestRestTemplate asCustomer() {
        return rest.withBasicAuth("customer", "customer123");
    }

    @Test
    void adminLifecycle_createBrowseBlockDeleteCascade() {
        // 1. City: create + update.
        CityResponse city = asAdmin()
                .postForEntity("/api/admin/cities", new CityRequest("Chennai", "Tamil Nadu", "India"), CityResponse.class)
                .getBody();
        assertThat(city).isNotNull();
        assertThat(city.name()).isEqualTo("Chennai");

        ResponseEntity<CityResponse> updated = asAdmin().exchange(
                "/api/admin/cities/" + city.id(), HttpMethod.PUT,
                new HttpEntity<>(new CityRequest("Chennai City", "Tamil Nadu", "India")), CityResponse.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().name()).isEqualTo("Chennai City");
        Long cityId = updated.getBody().id();

        // 2. Theater under that city.
        TheaterResponse theater = asAdmin()
                .postForEntity("/api/admin/theaters", new TheaterRequest(cityId, "Sathyam Cinemas", "Royapettah"), TheaterResponse.class)
                .getBody();
        assertThat(theater.cityId()).isEqualTo(cityId);
        assertThat(theater.cityName()).isEqualTo("Chennai City");
        Long theaterId = theater.id();

        // 3. Screen: 5 rows x 8 cols, back 2 rows premium -> 40 seats, 16 premium / 24 regular.
        ScreenResponse screen = asAdmin()
                .postForEntity("/api/admin/screens",
                        new ScreenRequest(theaterId, "Audi 1", 5, 8, Set.of("D", "E")), ScreenResponse.class)
                .getBody();
        assertThat(screen.totalSeats()).isEqualTo(40);
        Long screenId = screen.id();

        ResponseEntity<SeatResponse[]> seats = asAdmin()
                .getForEntity("/api/admin/screens/" + screenId + "/seats", SeatResponse[].class);
        assertThat(seats.getBody()).hasSize(40);
        long premiumCount = Arrays.stream(seats.getBody()).filter(s -> s.category().equals("PREMIUM")).count();
        long regularCount = Arrays.stream(seats.getBody()).filter(s -> s.category().equals("REGULAR")).count();
        assertThat(premiumCount).isEqualTo(16);
        assertThat(regularCount).isEqualTo(24);

        // 4. Movie.
        MovieResponse movie = asAdmin()
                .postForEntity("/api/admin/movies",
                        new MovieRequest("Test Feature", 120, "Tamil", "Drama", "U", "A test movie."), MovieResponse.class)
                .getBody();
        Long movieId = movie.id();

        // 5. Show: endTime must be startTime + duration.
        Instant startTime = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        ShowResponse show = asAdmin()
                .postForEntity("/api/admin/shows", new ShowRequest(movieId, screenId, startTime, null, null), ShowResponse.class)
                .getBody();
        assertThat(show.startTime()).isEqualTo(startTime);
        assertThat(show.endTime()).isEqualTo(startTime.plus(Duration.ofMinutes(120)));
        Long showId = show.id();

        ResponseEntity<ShowResponse> fetchedShow = asAdmin().getForEntity("/api/admin/shows/" + showId, ShowResponse.class);
        assertThat(fetchedShow.getBody().movieId()).isEqualTo(movieId);

        // 6. Browse endpoints see the new data immediately.
        ResponseEntity<ShowSeatResponse[]> availableSeats =
                asCustomer().getForEntity("/api/shows/" + showId + "/seats", ShowSeatResponse[].class);
        assertThat(availableSeats.getBody()).hasSize(40);
        assertThat(Arrays.stream(availableSeats.getBody()).allMatch(s -> s.status().equals("AVAILABLE"))).isTrue();

        ResponseEntity<ShowResponse[]> theaterShows =
                asCustomer().getForEntity("/api/theaters/" + theaterId + "/shows", ShowResponse[].class);
        assertThat(Arrays.stream(theaterShows.getBody()).anyMatch(s -> s.id().equals(showId))).isTrue();

        ResponseEntity<TheaterResponse[]> cityTheaters =
                asCustomer().getForEntity("/api/cities/" + cityId + "/theaters", TheaterResponse[].class);
        assertThat(Arrays.stream(cityTheaters.getBody()).anyMatch(t -> t.id().equals(theaterId))).isTrue();

        // 7. Deletes are blocked while the show exists: movie/screen are referenced
        // directly (RESTRICT); theater/city are blocked transitively through their
        // screens' seats, which the show's show_seats still reference.
        assertThat(asAdmin().exchange("/api/admin/movies/" + movieId, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(asAdmin().exchange("/api/admin/screens/" + screenId, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(asAdmin().exchange("/api/admin/theaters/" + theaterId, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(asAdmin().exchange("/api/admin/cities/" + cityId, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 8. Deleting the show frees everything up to cascade/delete cleanly, in order.
        assertThat(asAdmin().exchange("/api/admin/shows/" + showId, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(asAdmin().exchange("/api/admin/movies/" + movieId, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(asAdmin().exchange("/api/admin/screens/" + screenId, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(asAdmin().exchange("/api/admin/theaters/" + theaterId, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(asAdmin().exchange("/api/admin/cities/" + cityId, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(asAdmin().getForEntity("/api/admin/cities/" + cityId, ErrorResponse.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createCityWithBlankName_returns400WithConsistentErrorShape() {
        ResponseEntity<ErrorResponse> response = asAdmin().postForEntity(
                "/api/admin/cities", new CityRequest("", "Some State", "Some Country"), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).contains("name");
        assertThat(body.path()).isEqualTo("/api/admin/cities");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void getUnknownCity_returns404() {
        ResponseEntity<ErrorResponse> response = asAdmin().getForEntity("/api/admin/cities/999999999", ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("999999999");
    }

    @Test
    void createTheaterWithUnknownCity_returns404() {
        ResponseEntity<ErrorResponse> response = asAdmin().postForEntity(
                "/api/admin/theaters", new TheaterRequest(999999999L, "Ghost Theater", "Nowhere"), ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createScreenWithPremiumRowOutsideRange_returns400() {
        CityResponse city = asAdmin()
                .postForEntity("/api/admin/cities", new CityRequest("Pune", "Maharashtra", "India"), CityResponse.class)
                .getBody();
        TheaterResponse theater = asAdmin()
                .postForEntity("/api/admin/theaters", new TheaterRequest(city.id(), "Throwaway Theater", "Somewhere"), TheaterResponse.class)
                .getBody();

        ResponseEntity<ErrorResponse> response = asAdmin().postForEntity(
                "/api/admin/screens",
                new ScreenRequest(theater.id(), "Bad Screen", 3, 8, Set.of("Z")),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("Z");
    }

    @Test
    void customerCannotCreateCity_returns403() {
        ResponseEntity<String> response = asCustomer()
                .postForEntity("/api/admin/cities", new CityRequest("Nope City", "NA", "NA"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
