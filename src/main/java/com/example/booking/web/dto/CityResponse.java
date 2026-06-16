package com.example.booking.web.dto;

import com.example.booking.domain.City;

public record CityResponse(Long id, String name, String state, String country) {

    public static CityResponse from(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getState(), city.getCountry());
    }
}
