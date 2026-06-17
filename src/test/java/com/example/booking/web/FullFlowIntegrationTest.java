package com.example.booking.web;

import com.example.booking.AbstractIntegrationTest;
import com.example.booking.service.MockPaymentGatewayImpl;
import com.example.booking.web.dto.BookingResponse;
import com.example.booking.web.dto.CityResponse;
import com.example.booking.web.dto.ShowResponse;
import com.example.booking.web.dto.ShowSeatResponse;
import com.example.booking.web.dto.TheaterResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test exercising the full customer journey on seeded
 * data from V3__seed_data.sql (no test-created fixtures):
 *
 *   browse cities → pick theater → pick show → pick available seat
 *     → hold → book → CONFIRMED → cancel → CANCELLED with refund
 *
 * Also covers the payment-failure path: hold → book → PAYMENT_FAILED → seat
 * reverts to AVAILABLE.
 *
 * Segment 6: Hardening & Polish.
 */
class FullFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired MockPaymentGatewayImpl mockPaymentGateway;
    @Autowired JdbcTemplate jdbcTemplate;

    private TestRestTemplate customer;

    @BeforeEach
    void setUp() {
        mockPaymentGateway.setAlwaysFail(false);
        customer = rest.withBasicAuth("customer", "customer123");
    }

    @Test
    void happyPath_browseHoldBookCancel_fullJourney() {
        // — Browse: city list includes seeded cities —
        CityResponse[] cities = customer.getForEntity("/api/cities", CityResponse[].class).getBody();
        assertThat(cities).isNotNull().isNotEmpty();

        Long bengaluruId = Arrays.stream(cities)
                .filter(c -> "Bengaluru".equals(c.name()))
                .map(CityResponse::id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Bengaluru not found in seed data"));

        // — Theaters in Bengaluru —
        TheaterResponse[] theaters = customer
                .getForEntity("/api/cities/" + bengaluruId + "/theaters", TheaterResponse[].class)
                .getBody();
        assertThat(theaters).isNotNull().isNotEmpty();
        Long theaterId = theaters[0].id();

        // — Upcoming shows at that theater —
        ShowResponse[] shows = customer
                .getForEntity("/api/theaters/" + theaterId + "/shows", ShowResponse[].class)
                .getBody();
        assertThat(shows).isNotNull().isNotEmpty();
        Long showId = shows[0].id();

        // — Seat list: all seats start as AVAILABLE in fresh seeded show —
        ShowSeatResponse[] seats = customer
                .getForEntity("/api/shows/" + showId + "/seats", ShowSeatResponse[].class)
                .getBody();
        assertThat(seats).isNotNull().isNotEmpty();
        Long showSeatId = Arrays.stream(seats)
                .filter(s -> "AVAILABLE".equals(s.status()))
                .map(ShowSeatResponse::showSeatId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No AVAILABLE seat in show " + showId));

        // — Hold the seat —
        ResponseEntity<String> holdResp = customer.postForEntity(
                "/api/shows/" + showId + "/seats/" + showSeatId + "/hold", null, String.class);
        assertThat(holdResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Seat now shows as HELD
        ShowSeatResponse[] afterHold = customer
                .getForEntity("/api/shows/" + showId + "/seats", ShowSeatResponse[].class)
                .getBody();
        assertThat(afterHold).isNotNull();
        assertThat(Arrays.stream(afterHold)
                .filter(s -> s.showSeatId().equals(showSeatId))
                .findFirst()
                .orElseThrow()
                .status()).isEqualTo("HELD");

        // — Book the held seat —
        BookingResponse booking = postBooking(showId, showSeatId, UUID.randomUUID().toString());
        assertThat(booking).isNotNull();
        assertThat(booking.status().name()).isEqualTo("CONFIRMED");
        assertThat(booking.finalPrice()).isNotNull().isPositive();
        assertThat(booking.confirmedAt()).isNotNull();

        // Seat now shows as BOOKED
        ShowSeatResponse[] afterBook = customer
                .getForEntity("/api/shows/" + showId + "/seats", ShowSeatResponse[].class)
                .getBody();
        assertThat(afterBook).isNotNull();
        assertThat(Arrays.stream(afterBook)
                .filter(s -> s.showSeatId().equals(showSeatId))
                .findFirst()
                .orElseThrow()
                .status()).isEqualTo("BOOKED");

        // — Cancel the booking —
        ResponseEntity<BookingResponse> cancelResp = customer.postForEntity(
                "/api/bookings/" + booking.id() + "/cancel", null, BookingResponse.class);
        assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        BookingResponse cancelled = cancelResp.getBody();
        assertThat(cancelled).isNotNull();
        assertThat(cancelled.status().name()).isEqualTo("CANCELLED");
        assertThat(cancelled.cancelledAt()).isNotNull();
        // Refund amount is policy-driven (50% if 12-24 h out, 100% if >24 h).
        // The seeded show may be within 24 h depending on when tests run; just
        // verify a positive refund was issued rather than hard-coding the tier.
        assertThat(cancelled.refundAmount()).isPositive();

        // Seat is back to AVAILABLE
        ShowSeatResponse[] afterCancel = customer
                .getForEntity("/api/shows/" + showId + "/seats", ShowSeatResponse[].class)
                .getBody();
        assertThat(afterCancel).isNotNull();
        assertThat(Arrays.stream(afterCancel)
                .filter(s -> s.showSeatId().equals(showSeatId))
                .findFirst()
                .orElseThrow()
                .status()).isEqualTo("AVAILABLE");
    }

    @Test
    void paymentFailure_seatReturnsToAvailable() {
        // Pick a seeded show and a different available seat from it
        Long showId = jdbcTemplate.queryForObject(
                "SELECT s.id FROM shows s " +
                "JOIN show_seats ss ON ss.show_id = s.id " +
                "WHERE ss.status = 'AVAILABLE' " +
                "  AND s.pricing_tier_id IS NOT NULL " +
                "  AND s.start_time > now() " +
                "LIMIT 1",
                Long.class);
        assertThat(showId).as("must have a seeded show with pricing tier and available seats").isNotNull();

        Long showSeatId = jdbcTemplate.queryForObject(
                "SELECT id FROM show_seats WHERE show_id = ? AND status = 'AVAILABLE' LIMIT 1",
                Long.class, showId);

        // Hold the seat
        ResponseEntity<String> holdResp = customer.postForEntity(
                "/api/shows/" + showId + "/seats/" + showSeatId + "/hold", null, String.class);
        assertThat(holdResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Simulate payment failure
        mockPaymentGateway.setAlwaysFail(true);
        try {
            BookingResponse booking = postBooking(showId, showSeatId, UUID.randomUUID().toString());
            assertThat(booking).isNotNull();
            assertThat(booking.status().name()).isEqualTo("PAYMENT_FAILED");
        } finally {
            mockPaymentGateway.setAlwaysFail(false);
        }

        // Seat must be back to AVAILABLE after payment failure
        String seatStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM show_seats WHERE id = ?", String.class, showSeatId);
        assertThat(seatStatus).isEqualTo("AVAILABLE");
    }

    @Test
    void holdExpiry_cannotBookExpiredHold() {
        Long showId = jdbcTemplate.queryForObject(
                "SELECT s.id FROM shows s " +
                "JOIN show_seats ss ON ss.show_id = s.id " +
                "WHERE ss.status = 'AVAILABLE' " +
                "  AND s.pricing_tier_id IS NOT NULL " +
                "  AND s.start_time > now() " +
                "LIMIT 1",
                Long.class);
        Long showSeatId = jdbcTemplate.queryForObject(
                "SELECT id FROM show_seats WHERE show_id = ? AND status = 'AVAILABLE' LIMIT 1",
                Long.class, showId);

        // Hold it
        customer.postForEntity("/api/shows/" + showId + "/seats/" + showSeatId + "/hold", null, String.class);

        // Artificially expire the hold in the DB
        jdbcTemplate.update(
                "UPDATE show_seats SET hold_expires_at = now() - interval '1 minute' WHERE id = ?",
                showSeatId);

        try {
            // Attempt to book the expired hold → 409
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Idempotency-Key", UUID.randomUUID().toString());
            headers.set("Content-Type", "application/json");
            String body = "{\"showId\":" + showId + ",\"showSeatId\":" + showSeatId + ",\"discountCode\":null}";
            ResponseEntity<String> resp = customer.exchange(
                    "/api/bookings", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        } finally {
            // Restore to AVAILABLE so the shared DB is clean for other test classes
            jdbcTemplate.update(
                    "UPDATE show_seats SET status = 'AVAILABLE', held_by = NULL, hold_expires_at = NULL WHERE id = ?",
                    showSeatId);
        }
    }

    // — helper —

    private BookingResponse postBooking(Long showId, Long showSeatId, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Idempotency-Key", idempotencyKey);
        headers.set("Content-Type", "application/json");
        String body = "{\"showId\":" + showId + ",\"showSeatId\":" + showSeatId + ",\"discountCode\":null}";
        return customer.exchange("/api/bookings", HttpMethod.POST,
                new HttpEntity<>(body, headers), BookingResponse.class).getBody();
    }
}
