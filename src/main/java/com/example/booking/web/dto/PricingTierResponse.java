package com.example.booking.web.dto;

import com.example.booking.domain.PricingTier;

import java.math.BigDecimal;
import java.time.Instant;

public record PricingTierResponse(
        Long id,
        String name,
        BigDecimal regularPrice,
        BigDecimal premiumPrice,
        BigDecimal weekendMultiplier,
        Instant createdAt
) {
    public static PricingTierResponse from(PricingTier tier) {
        return new PricingTierResponse(tier.getId(), tier.getName(),
                tier.getRegularPrice(), tier.getPremiumPrice(),
                tier.getWeekendMultiplier(), tier.getCreatedAt());
    }
}
