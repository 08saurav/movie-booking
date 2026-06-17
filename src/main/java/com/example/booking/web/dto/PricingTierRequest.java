package com.example.booking.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PricingTierRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.00") BigDecimal regularPrice,
        @NotNull @DecimalMin("0.00") BigDecimal premiumPrice,
        @NotNull @DecimalMin("1.00") BigDecimal weekendMultiplier
) {}
