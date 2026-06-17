package com.example.booking.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RefundTierDto(
        @NotNull @Min(0) Integer hoursBeforeShow,
        @NotNull @Min(0) @Max(100) Integer refundPercent
) {}
