package com.example.booking.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TheaterRequest(
        @NotNull Long cityId,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 300) String address
) {
}
