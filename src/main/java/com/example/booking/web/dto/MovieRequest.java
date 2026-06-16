package com.example.booking.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MovieRequest(
        @NotBlank @Size(max = 200) String title,
        @Min(1) int durationMinutes,
        @Size(max = 50) String language,
        @Size(max = 50) String genre,
        @Size(max = 10) String rating,
        @Size(max = 1000) String description
) {
}
