package com.example.booking.web;

import com.example.booking.service.BookingService;
import com.example.booking.web.dto.BookingRequest;
import com.example.booking.web.dto.BookingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

/**
 * Customer booking endpoints. Segment 4: Pricing, Payment & Confirmation.
 *
 * The X-Idempotency-Key header makes POST /api/bookings safe to retry:
 * submitting the same key twice returns the existing booking without
 * re-charging. Keys should be unique per booking attempt (e.g. UUID v4).
 */
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Customer: Bookings", description = "Book held seats, view booking history")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(summary = "Book a held seat",
            description = "The seat must be in HELD state for this customer (via POST /api/shows/{showId}/seats/{seatId}/hold). "
                    + "X-Idempotency-Key is required: re-submitting the same key returns the existing booking. "
                    + "Returns CONFIRMED on payment success or PAYMENT_FAILED on payment failure (seat released).")
    public ResponseEntity<BookingResponse> book(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            Authentication authentication) {
        BookingResponse response = bookingService.book(authentication.getName(), request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List my bookings", description = "Returns all bookings for the authenticated customer, newest first.")
    public List<BookingResponse> list(Authentication authentication) {
        return bookingService.listForCustomer(authentication.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a booking by id", description = "Returns 404 if the booking belongs to a different customer.")
    public BookingResponse getById(@PathVariable Long id, Authentication authentication) {
        return bookingService.getForCustomer(id, authentication.getName());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a confirmed booking",
            description = "Only CONFIRMED bookings can be cancelled. Refund amount is determined by the show's refund policy. "
                    + "Returns 404 if the booking belongs to a different customer.")
    public BookingResponse cancel(@PathVariable Long id, Authentication authentication) {
        return bookingService.cancel(id, authentication.getName(), false);
    }
}
