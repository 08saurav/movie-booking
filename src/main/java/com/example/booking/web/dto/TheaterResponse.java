package com.example.booking.web.dto;

import com.example.booking.domain.Theater;

public record TheaterResponse(Long id, String name, String address, Long cityId, String cityName) {

    public static TheaterResponse from(Theater theater) {
        return new TheaterResponse(
                theater.getId(),
                theater.getName(),
                theater.getAddress(),
                theater.getCity().getId(),
                theater.getCity().getName());
    }
}
