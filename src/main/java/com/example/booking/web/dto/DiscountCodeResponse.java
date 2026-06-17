package com.example.booking.web.dto;

import com.example.booking.domain.DiscountCode;
import com.example.booking.domain.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record DiscountCodeResponse(
        Long id,
        String code,
        DiscountType discountType,
        BigDecimal value,
        Integer maxUses,
        int currentUses,
        Instant validFrom,
        Instant validTo,
        boolean active,
        Instant createdAt
) {
    public static DiscountCodeResponse from(DiscountCode dc) {
        return new DiscountCodeResponse(dc.getId(), dc.getCode(), dc.getDiscountType(),
                dc.getValue(), dc.getMaxUses(), dc.getCurrentUses(),
                dc.getValidFrom(), dc.getValidTo(), dc.isActive(), dc.getCreatedAt());
    }
}
