package com.example.booking.web.dto;

import com.example.booking.domain.Seat;

public record SeatResponse(Long id, String rowLabel, int seatNumber, String category) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(), seat.getCategory().name());
    }
}
