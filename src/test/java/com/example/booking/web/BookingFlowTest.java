package com.example.booking.web;

import com.example.booking.AbstractIntegrationTest;
import com.example.booking.domain.City;
import com.example.booking.domain.Movie;
import com.example.booking.domain.PricingTier;
import com.example.booking.domain.Screen;
import com.example.booking.domain.Seat;
import com.example.booking.domain.SeatCategory;
import com.example.booking.domain.Show;
import com.example.booking.domain.ShowSeat;
import com.example.booking.domain.ShowSeatStatus;
import com.example.booking.domain.Theater;
import com.example.booking.repository.CityRepository;
import com.example.booking.repository.MovieRepository;
import com.example.booking.repository.PricingTierRepository;
import com.example.booking.repository.ScreenRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.ShowRepository;
import com.example.booking.repository.ShowSeatRepository;
import com.example.booking.repository.TheaterRepository;
import com.example.booking.service.MockPaymentGatewayImpl;
import com.example.booking.web.dto.BookingRequest;
import com.example.booking.web.dto.BookingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the full booking flow: hold → book → confirm.
 * Covers happy path, idempotency, payment failure, and admin/customer list views.
 *
 * Uses Testcontainers (real PostgreSQL) via AbstractIntegrationTest.
 * Each test creates its own isolated show/seat data in @BeforeEach.
 *
 * Segment 4: Pricing, Payment & Confirmation.
 */
class BookingFlowTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired CityRepository cityRepository;
    @Autowired TheaterRepository theaterRepository;
    @Autowired ScreenRepository screenRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired MovieRepository movieRepository;
    @Autowired ShowRepository showRepository;
    @Autowired ShowSeatRepository showSeatRepository;
    @Autowired PricingTierRepository pricingTierRepository;
    @Autowired MockPaymentGatewayImpl mockPaymentGateway;

    private Long showId;
    private Long showSeatId;

    @BeforeEach
    void setUp() {
        mockPaymentGateway.setAlwaysFail(false);

        City city = cityRepository.save(new City("Booking Test City", null, null));
        Theater theater = theaterRepository.save(new Theater(city, "Booking Test Theater", null));
        Screen screen = screenRepository.save(new Screen(theater, "Screen B4", 2, 5));
        Seat seat = seatRepository.save(new Seat(screen, "A", 1, SeatCategory.REGULAR));

        Movie movie = movieRepository.save(new Movie("Booking Test Movie", 90, null, null, null, null));
        PricingTier tier = pricingTierRepository.save(
                new PricingTier("Test Tier", new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("1.25")));

        Instant startTime = Instant.now().plus(7, ChronoUnit.DAYS);
        Instant endTime = startTime.plus(90, ChronoUnit.MINUTES);
        Show show = showRepository.save(new Show(movie, screen, startTime, endTime, tier));
        ShowSeat showSeat = showSeatRepository.save(new ShowSeat(show, seat, ShowSeatStatus.AVAILABLE));

        this.showId = show.getId();
        this.showSeatId = showSeat.getId();
    }

    @Test
    void happyPath_holdThenBook_returnsConfirmed() {
        TestRestTemplate customer = rest.withBasicAuth("customer", "customer123");

        // Step 1: Hold the seat
        ResponseEntity<String> holdResponse = customer.postForEntity(
                "/api/shows/" + showId + "/seats/" + showSeatId + "/hold", null, String.class);
        assertThat(holdResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 2: Book the held seat
        String idempotencyKey = UUID.randomUUID().toString();
        BookingResponse booking = postBooking(customer, showId, showSeatId, null, idempotencyKey);

        assertThat(booking.status().name()).isEqualTo("CONFIRMED");
        assertThat(booking.customer()).isEqualTo("customer");
        assertThat(booking.showId()).isEqualTo(showId);
        assertThat(booking.showSeatId()).isEqualTo(showSeatId);
        assertThat(booking.finalPrice()).isGreaterThan(BigDecimal.ZERO);
        assertThat(booking.confirmedAt()).isNotNull();
    }

    @Test
    void idempotency_sameKeyTwice_returnsSameBooking() {
        TestRestTemplate customer = rest.withBasicAuth("customer", "customer123");

        // Hold
        customer.postForEntity("/api/shows/" + showId + "/seats/" + showSeatId + "/hold", null, String.class);

        // Book once
        String idempotencyKey = UUID.randomUUID().toString();
        BookingResponse first = postBooking(customer, showId, showSeatId, null, idempotencyKey);
        assertThat(first.status().name()).isEqualTo("CONFIRMED");

        // Book again with the same key — must return the same booking
        BookingResponse second = postBooking(customer, showId, showSeatId, null, idempotencyKey);
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.status()).isEqualTo(first.status());
    }

    @Test
    void customerListBookings_includesOwnBooking() {
        TestRestTemplate customer = rest.withBasicAuth("customer", "customer123");

        customer.postForEntity("/api/shows/" + showId + "/seats/" + showSeatId + "/hold", null, String.class);
        BookingResponse booking = postBooking(customer, showId, showSeatId, null, UUID.randomUUID().toString());

        ResponseEntity<BookingResponse[]> listResponse = customer.getForEntity("/api/bookings", BookingResponse[].class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull();
        assertThat(listResponse.getBody()).anyMatch(b -> b.id().equals(booking.id()));
    }

    @Test
    void adminListBookings_seesAllBookings() {
        TestRestTemplate customer = rest.withBasicAuth("customer", "customer123");
        customer.postForEntity("/api/shows/" + showId + "/seats/" + showSeatId + "/hold", null, String.class);
        BookingResponse booking = postBooking(customer, showId, showSeatId, null, UUID.randomUUID().toString());

        TestRestTemplate admin = rest.withBasicAuth("admin", "admin123");
        ResponseEntity<String> adminResponse = admin.getForEntity("/api/admin/bookings", String.class);
        assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminResponse.getBody()).contains(booking.id().toString());
    }

    @Test
    void paymentFailure_seatReleasedToAvailable() {
        mockPaymentGateway.setAlwaysFail(true);
        TestRestTemplate customer = rest.withBasicAuth("customer", "customer123");

        // Hold
        customer.postForEntity("/api/shows/" + showId + "/seats/" + showSeatId + "/hold", null, String.class);

        // Book — payment will fail
        ResponseEntity<BookingResponse> response = postBookingRaw(customer, showId, showSeatId, null, UUID.randomUUID().toString());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status().name()).isEqualTo("PAYMENT_FAILED");

        // Seat must be back to AVAILABLE
        ShowSeat seat = showSeatRepository.findById(showSeatId).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);
    }

    @Test
    void bookWithoutHold_returns409() {
        TestRestTemplate customer = rest.withBasicAuth("customer", "customer123");
        // Seat is AVAILABLE (not HELD) — booking should fail
        ResponseEntity<String> response = postBookingRawString(customer, showId, showSeatId, null, UUID.randomUUID().toString());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void discountCode_appliedCorrectly() {
        // Create a 10% discount code via admin
        TestRestTemplate admin = rest.withBasicAuth("admin", "admin123");
        String createCode = """
                {"code":"TEST10","discountType":"PERCENTAGE","value":10,"active":true}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        admin.exchange("/api/admin/discount-codes", HttpMethod.POST,
                new HttpEntity<>(createCode, headers), String.class);

        TestRestTemplate customer = rest.withBasicAuth("customer", "customer123");
        customer.postForEntity("/api/shows/" + showId + "/seats/" + showSeatId + "/hold", null, String.class);

        BookingResponse booking = postBooking(customer, showId, showSeatId, "TEST10", UUID.randomUUID().toString());

        assertThat(booking.status().name()).isEqualTo("CONFIRMED");
        assertThat(booking.discountCode()).isEqualTo("TEST10");
        assertThat(booking.discountAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(booking.finalPrice()).isLessThan(booking.basePrice());
    }

    // — helpers —

    private BookingResponse postBooking(TestRestTemplate client, Long showId, Long showSeatId,
                                         String discountCode, String idempotencyKey) {
        return postBookingRaw(client, showId, showSeatId, discountCode, idempotencyKey).getBody();
    }

    private ResponseEntity<BookingResponse> postBookingRaw(TestRestTemplate client, Long showId, Long showSeatId,
                                                            String discountCode, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Idempotency-Key", idempotencyKey);
        headers.set("Content-Type", "application/json");
        BookingRequest body = new BookingRequest(showId, showSeatId, discountCode);
        return client.exchange("/api/bookings", HttpMethod.POST, new HttpEntity<>(body, headers), BookingResponse.class);
    }

    private ResponseEntity<String> postBookingRawString(TestRestTemplate client, Long showId, Long showSeatId,
                                                         String discountCode, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Idempotency-Key", idempotencyKey);
        headers.set("Content-Type", "application/json");
        BookingRequest body = new BookingRequest(showId, showSeatId, discountCode);
        return client.exchange("/api/bookings", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }
}
