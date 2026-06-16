package com.example.booking.web.dto;

import com.example.booking.domain.ShowSeat;

public record ShowSeatResponse(
        Long showSeatId,
        Long seatId,
        String rowLabel,
        int seatNumber,
        String category,
        String status
) {

    public static ShowSeatResponse from(ShowSeat showSeat) {
        var seat = showSeat.getSeat();
        return new ShowSeatResponse(
                showSeat.getId(),
                seat.getId(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getCategory().name(),
                showSeat.getStatus().name());
    }
}
