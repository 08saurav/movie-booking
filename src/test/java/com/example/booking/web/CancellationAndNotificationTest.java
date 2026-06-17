package com.example.booking.web;

import com.example.booking.AbstractIntegrationTest;
import com.example.booking.domain.City;
import com.example.booking.domain.Movie;
import com.example.booking.domain.PricingTier;
import com.example.booking.domain.RefundPolicy;
import com.example.booking.domain.RefundTier;
import com.example.booking.domain.Screen;
import com.example.booking.domain.Seat;
import com.example.booking.domain.SeatCategory;
import com.example.booking.domain.Show;
import com.example.booking.domain.ShowSeat;
import com.example.booking.domain.ShowSeatStatus;
import com.example.booking.domain.Theater;
import com.example.booking.event.NotificationListener;
import com.example.booking.repository.CityRepository;
import com.example.booking.repository.MovieRepository;
import com.example.booking.repository.PricingTierRepository;
import com.example.booking.repository.RefundPolicyRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Segment 5: cancellation, refund calculation, and async notifications.
 *
 * Key test: proves that @TransactionalEventListener(AFTER_COMMIT) + @Async means
 * the notification listener runs on a different thread than the test thread.
 *
 * Segment 5: Cancellation, Refunds & Notifications.
 */
class CancellationAndNotificationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired CityRepository cityRepository;
    @Autowired TheaterRepository theaterRepository;
    @Autowired ScreenRepository screenRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired MovieRepository movieRepository;
    @Autowired ShowRepository showRepository;
    @Autowired ShowSeatRepository showSeatRepository;
    @Autowired PricingTierRepository pricingTierRepository;
    @Autowired RefundPolicyRepository refundPolicyRepository;
    @Autowired MockPaymentGatewayImpl mockPaymentGateway;

    private Long showId;
    private Long showSeatId;
    private Long show2Id;
    private Long showSeat2Id;

    @BeforeEach
    void setUp() {
        mockPaymentGateway.setAlwaysFail(false);
        NotificationListener.confirmedThreads.clear();
        NotificationListener.cancelledThreads.clear();

        City city = cityRepository.save(new City("Cancel Test City", null, null));
        Theater theater = theaterRepository.save(new Theater(city, "Cancel Test Theater", null));
        Screen screen = screenRepository.save(new Screen(theater, "Screen C5", 2, 5));

        Seat seat1 = seatRepository.save(new Seat(screen, "A", 1, SeatCategory.REGULAR));
        Seat seat2 = seatRepository.save(new Seat(screen, "A", 2, SeatCategory.REGULAR));

        Movie movie = movieRepository.save(new Movie("Cancel Test Movie", 90, null, null, null, null));
        PricingTier tier = pricingTierRepository.save(
                new PricingTier("Cancel Tier", new BigDecimal("200.00"), new BigDecimal("400.00"), new BigDecimal("1.00")));

        RefundPolicy policy = refundPolicyRepository.save(new RefundPolicy("Test Policy", false,
                List.of(new RefundTier(24, 100), new RefundTier(12, 50), new RefundTier(0, 0))));

        // Show 1: far in future → eligible for full refund
        Instant start1 = Instant.now().plus(30, ChronoUnit.DAYS);
        Show show1 = showRepository.save(new Show(movie, screen, start1, start1.plus(90, ChronoUnit.MINUTES), tier, policy));
        ShowSeat ss1 = showSeatRepository.save(new ShowSeat(show1, seat1, ShowSeatStatus.AVAILABLE));
        this.showId = show1.getId();
        this.showSeatId = ss1.getId();

        // Show 2: also far future (for second cancellation test)
        Instant start2 = Instant.now().plus(31, ChronoUnit.DAYS);
        Show show2 = showRepository.save(new Show(movie, screen, start2, start2.plus(90, ChronoUnit.MINUTES), tier, policy));
        ShowSeat ss2 = showSeatRepository.save(new ShowSeat(show2, seat2, ShowSeatStatus.AVAILABLE));
        this.show2Id = show2.getId();
        this.showSeat2Id = ss2.getId();
    }

    @Test
    void cancel_confirmedBooking_returnsRefundAmount() {
        TestRestTemplate customer = rest.withBasicAuth("customer", "customer123");

        // Hold and book
        customer.postForEntity("/api/shows/" + showId + "/seats/" + showSeatId + "/hold", null, String.class);
        BookingResponse booking = postBooking(customer, showId, showSeatId, UUID.randomUUID().toString());
        assertThat(booking.status().name()).isEqualTo("CONFIRMED");

        // Cancel
        ResponseEntity<BookingResponse> cancelResp = customer.postForEntity(
                "/api/bookings/" + booking.id() + "/cancel", null, BookingResponse.class);

        assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        BookingResponse cancelled = cancelResp.getBody();
        assertThat(cancelled).isNotNull();
        assertThat(cancelled.status().name()).isEqualTo("CANCELLED");
        assertThat(cancelled.cancelledAt()).isNotNull();
        // Show is 30 days away → >24h → 100% refund
        assertThat(cancelled.refundAmount()).isEqualByComparingTo(booking.finalPrice());
    }

    @Test
    void cancel_alreadyCancelled_returns400() {
        TestRestTemplate customer = rest.withBasicAuth("customer", "customer123");

        customer.postForEntity("/api/shows/" + showId + "/seats/" + showSeatId + "/hold", null, String.class);
        BookingResponse booking = postBooking(customer, showId, showSeatId, UUID.randomUUID().toString());

        customer.postForEntity("/api/bookings/" + booking.id() + "/cancel", null, BookingResponse.class);
        // Second cancel attempt
        ResponseEntity<String> resp = customer.postForEntity(
                "/api/bookings/" + booking.id() + "/cancel", null, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void adminCancel_anyBooking_succeeds() {
        TestRestTemplate customer = rest.withBasicAuth("customer", "customer123");
        customer.postForEntity("/api/shows/" + show2Id + "/seats/" + showSeat2Id + "/hold", null, String.class);
        BookingResponse booking = postBooking(customer, show2Id, showSeat2Id, UUID.randomUUID().toString());

        TestRestTemplate admin = rest.withBasicAuth("admin", "admin123");
        ResponseEntity<BookingResponse> cancelResp = admin.postForEntity(
                "/api/admin/bookings/" + booking.id() + "/cancel", null, BookingResponse.class);

        assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelResp.getBody().status().name()).isEqualTo("CANCELLED");
    }

    @Test
    void notification_confirmedBooking_runsOnDifferentThread() throws InterruptedException {
        TestRestTemplate customer = rest.withBasicAuth("customer", "customer123");
        String testThread = Thread.currentThread().getName();

        customer.postForEntity("/api/shows/" + showId + "/seats/" + showSeatId + "/hold", null, String.class);
        postBooking(customer, showId, showSeatId, UUID.randomUUID().toString());

        // Block until the async listener offers its thread name, or fail after 5 s
        String confirmedThread = NotificationListener.confirmedThreads.poll(5, TimeUnit.SECONDS);

        assertThat(confirmedThread)
                .as("async confirmed-notification thread name")
                .isNotNull()
                .isNotEqualTo(testThread)
                .startsWith("notification-");
    }

    @Test
    void notification_cancelledBooking_runsOnDifferentThread() throws InterruptedException {
        TestRestTemplate customer = rest.withBasicAuth("customer", "customer123");
        String testThread = Thread.currentThread().getName();

        customer.postForEntity("/api/shows/" + show2Id + "/seats/" + showSeat2Id + "/hold", null, String.class);
        BookingResponse booking = postBooking(customer, show2Id, showSeat2Id, UUID.randomUUID().toString());
        customer.postForEntity("/api/bookings/" + booking.id() + "/cancel", null, BookingResponse.class);

        // Discard the confirmed-notification that fired when the booking was created
        NotificationListener.confirmedThreads.poll(5, TimeUnit.SECONDS);
        // Block until the cancel notification fires
        String cancelledThread = NotificationListener.cancelledThreads.poll(5, TimeUnit.SECONDS);

        assertThat(cancelledThread)
                .as("async cancelled-notification thread name")
                .isNotNull()
                .isNotEqualTo(testThread)
                .startsWith("notification-");
    }

    // — helper —

    private BookingResponse postBooking(TestRestTemplate client, Long showId, Long showSeatId, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Idempotency-Key", idempotencyKey);
        headers.set("Content-Type", "application/json");
        BookingRequest body = new BookingRequest(showId, showSeatId, null);
        return client.exchange("/api/bookings", HttpMethod.POST,
                new HttpEntity<>(body, headers), BookingResponse.class).getBody();
    }
}
