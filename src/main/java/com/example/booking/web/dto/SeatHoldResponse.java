package com.example.booking.web.dto;

import com.example.booking.domain.ShowSeat;

import java.time.Instant;

/**
 * Response after a successful hold operation, including the hold expiry time
 * so the customer knows when they must book or release the hold.
 */
public record SeatHoldResponse(
        Long showSeatId,
        Long seatId,
        String rowLabel,
        int seatNumber,
        String category,
        Instant holdExpiresAt
) {

    public static SeatHoldResponse from(ShowSeat showSeat) {
        var seat = showSeat.getSeat();
        return new SeatHoldResponse(
                showSeat.getId(),
                seat.getId(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getCategory().name(),
                showSeat.getHoldExpiresAt());
    }
}
