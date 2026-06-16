package com.example.booking.web.dto;

import com.example.booking.domain.Screen;

public record ScreenResponse(Long id, String name, Long theaterId, int totalRows, int totalCols, int totalSeats) {

    public static ScreenResponse from(Screen screen) {
        return new ScreenResponse(
                screen.getId(),
                screen.getName(),
                screen.getTheater().getId(),
                screen.getTotalRows(),
                screen.getTotalCols(),
                screen.getTotalRows() * screen.getTotalCols());
    }
}
