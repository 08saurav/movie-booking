package com.example.booking.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CityRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 120) String state,
        @Size(max = 120) String country
) {
}
