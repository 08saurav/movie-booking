package com.example.booking.web;

import com.example.booking.AbstractIntegrationTest;
import com.example.booking.domain.City;
import com.example.booking.domain.Movie;
import com.example.booking.domain.Screen;
import com.example.booking.domain.Seat;
import com.example.booking.domain.SeatCategory;
import com.example.booking.domain.Show;
import com.example.booking.domain.ShowSeat;
import com.example.booking.domain.ShowSeatStatus;
import com.example.booking.domain.Theater;
import com.example.booking.repository.CityRepository;
import com.example.booking.repository.MovieRepository;
import com.example.booking.repository.ScreenRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.ShowRepository;
import com.example.booking.repository.ShowSeatRepository;
import com.example.booking.repository.TheaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Concurrency test: N threads simultaneously attempt to hold the same seat.
 * Exactly 1 succeeds (200), the other N-1 fail (409 Conflict).
 *
 * This test MUST use Testcontainers (real PostgreSQL) because H2 does not have
 * the same row-level locking semantics. The atomic conditional UPDATE closes
 * the TOCTOU window at the database level; this test verifies that behavior.
 *
 * Uses TestRestTemplate (thread-safe) rather than MockMvc for concurrent testing.
 *
 * Segment 3: Concurrency Core.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SeatHoldConcurrencyTest extends AbstractIntegrationTest {

    private static final int CONCURRENT_ATTEMPTS = 10;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    private Long showId;
    private Long showSeatId;

    @BeforeEach
    void setUp() {
        // Create minimal catalog: city -> theater -> screen -> movie -> show -> showseat
        City city = cityRepository.save(new City("Test City", null, null));
        Theater theater = theaterRepository.save(new Theater(city, "Test Theater", null));
        Screen screen = screenRepository.save(new Screen(theater, "Screen 1", 3, 10));

        // Create one seat at row A, col 1, category REGULAR
        Seat seat = new Seat(screen, "A", 1, SeatCategory.REGULAR);
        seat = seatRepository.save(seat);

        Movie movie = movieRepository.save(new Movie("Test Movie", 120, null, null, null, null));
        Instant startTime = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant endTime = startTime.plus(2, ChronoUnit.HOURS);
        Show show = showRepository.save(new Show(movie, screen, startTime, endTime));

        // Create one ShowSeat for the seat on this show, status AVAILABLE
        ShowSeat showSeat = new ShowSeat(show, seat, ShowSeatStatus.AVAILABLE);
        showSeat = showSeatRepository.save(showSeat);

        this.showId = show.getId();
        this.showSeatId = showSeat.getId();
    }

    /**
     * Fire N threads simultaneously, all attempting to hold the same seat.
     * Exactly 1 should succeed (HTTP 200), and exactly N-1 should fail (HTTP 409).
     *
     * This proves the atomic conditional UPDATE closes the race window:
     * the database serializes concurrent attempts, and only one winner emerges.
     */
    @Test
    void testConcurrentSeatHoldOnlyOneWins() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(CONCURRENT_ATTEMPTS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherCount = new AtomicInteger(0);

        // Fire N threads
        for (int i = 0; i < CONCURRENT_ATTEMPTS; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    // Wait for the starting signal
                    startLatch.await();

                    // All threads use the same in-memory customer (for simplicity).
                    // The atomic UPDATE serializes concurrent attempts regardless of user.
                    TestRestTemplate customer = restTemplate.withBasicAuth("customer", "customer123");
                    String url = "/api/shows/" + showId + "/seats/" + showSeatId + "/hold";

                    ResponseEntity<String> response = customer.postForEntity(url, null, String.class);
                    int status = response.getStatusCode().value();

                    if (status == 200) {
                        successCount.incrementAndGet();
                    } else if (status == 409) {
                        conflictCount.incrementAndGet();
                    } else {
                        otherCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        // Fire the starting gun: all threads attempt to hold simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        endLatch.await();

        // Verify exactly 1 won and N-1 lost
        assertEquals(1, successCount.get(), "Exactly 1 thread should hold the seat");
        assertEquals(CONCURRENT_ATTEMPTS - 1, conflictCount.get(),
                "Exactly " + (CONCURRENT_ATTEMPTS - 1) + " threads should fail with 409 Conflict");

        // Verify the seat is now HELD (not AVAILABLE)
        ShowSeat afterHold = showSeatRepository.findById(showSeatId)
                .orElseThrow();
        assertEquals(ShowSeatStatus.HELD, afterHold.getStatus());
        assertNotNull(afterHold.getHeldBy(), "Seat should be held by someone");
        assertNotNull(afterHold.getHoldExpiresAt(), "Hold should have an expiry time");
    }
}
