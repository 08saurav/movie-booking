package com.example.booking.web;

import com.example.booking.service.SeatHoldService;
import com.example.booking.web.dto.SeatHoldResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @PostMapping("/api/shows/{showId}/seats/{seatId}/hold")
    @Operation(summary = "Hold a seat",
            description = "Hold a seat for this customer. Returns 200 with hold expiry time, or 409 if the seat is not available.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Hold created — seat reserved for this customer until holdExpiresAt"),
        @ApiResponse(responseCode = "404", description = "Show or seat not found"),
        @ApiResponse(responseCode = "409", description = "Seat is already held or booked — try a different seat")
    })
    public ResponseEntity<SeatHoldResponse> holdSeat(
            @PathVariable Long showId,
            @PathVariable Long seatId,
            Authentication authentication) {
        String userId = authentication.getName();
        SeatHoldResponse response = seatHoldService.holdSeat(showId, seatId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/api/shows/{showId}/seats/{seatId}/hold")
    @Operation(summary = "Release a hold",
            description = "Release a hold on a seat you are holding. Returns 204 if successful.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Hold released — seat is now available for others"),
        @ApiResponse(responseCode = "404", description = "Seat not found or not held by this customer"),
        @ApiResponse(responseCode = "409", description = "Seat is not in HELD state")
    })
    public ResponseEntity<Void> releaseSeat(
            @PathVariable Long showId,
            @PathVariable Long seatId,
            Authentication authentication) {
        String userId = authentication.getName();
        seatHoldService.releaseSeat(seatId, userId);
        return ResponseEntity.noContent().build();
    }
}
