package com.example.booking.web.dto;

import com.example.booking.domain.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record DiscountCodeRequest(
        @NotBlank String code,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0.01") BigDecimal value,
        Integer maxUses,
        Instant validFrom,
        Instant validTo,
        boolean active
) {}
