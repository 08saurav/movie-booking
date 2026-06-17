package com.example.booking.web;

import com.example.booking.service.SeatHoldService;
import com.example.booking.web.dto.SeatHoldResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer seat hold endpoints. Segment 3: Concurrency Core.
 *
 * Hold mechanism: atomic conditional UPDATE at the database level ensures
 * that exactly one customer gets the seat if multiple attempt simultaneously.
 * Race-free; no application-level locking needed.
 */
@RestController
@Tag(name = "Customer: Seat Holds", description = "Hold and release seats with automatic expiry")
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    public SeatHoldController(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    /**
     * Hold a seat for time-bound booking. Returns 200 with hold details (including
     * expiry time) if successful, or 409 if the seat is taken/held by someone else.
     */
    @PostMapping("/api/shows/{showId}/seats/{seatId}/hold")
    @Operation(summary = "Hold a seat",
            description = "Hold a seat for this customer. Returns 200 with hold expiry time, or 409 if the seat is not available.")
    public ResponseEntity<SeatHoldResponse> holdSeat(
            @PathVariable Long showId,
            @PathVariable Long seatId,
            Authentication authentication) {
        String userId = authentication.getName();
        SeatHoldResponse response = seatHoldService.holdSeat(showId, seatId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Release a hold early (customer cancels the hold before booking).
     * Returns 204 No Content on success, or 404/409 if the hold doesn't exist
     * or is held by a different customer.
     */
    @DeleteMapping("/api/shows/{showId}/seats/{seatId}/hold")
    @Operation(summary = "Release a hold",
            description = "Release a hold on a seat you are holding. Returns 204 if successful.")
    public ResponseEntity<Void> releaseSeat(
            @PathVariable Long showId,
            @PathVariable Long seatId,
            Authentication authentication) {
        String userId = authentication.getName();
        seatHoldService.releaseSeat(seatId, userId);
        return ResponseEntity.noContent().build();
    }
}
