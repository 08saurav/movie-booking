package com.example.booking.web.dto;

import com.example.booking.domain.ShowSeat;

import java.math.BigDecimal;

public record ShowSeatResponse(
        Long showSeatId,
        Long seatId,
        String rowLabel,
        int seatNumber,
        String category,
        /** Effective price before any discount code (null if show has no pricing tier). */
        BigDecimal price,
        String status,
        /** True only if the authenticated customer is the one currently holding this seat. */
        boolean heldByMe
) {

    public static ShowSeatResponse from(ShowSeat showSeat, BigDecimal price, String currentUser) {
        var seat = showSeat.getSeat();
        return new ShowSeatResponse(
                showSeat.getId(),
                seat.getId(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getCategory().name(),
                price,
                showSeat.getStatus().name(),
                currentUser != null && currentUser.equals(showSeat.getHeldBy()));
    }
}
