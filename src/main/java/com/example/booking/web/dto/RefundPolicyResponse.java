package com.example.booking.web.dto;

import com.example.booking.domain.RefundPolicy;

import java.time.Instant;
import java.util.List;

public record RefundPolicyResponse(
        Long id,
        String name,
        boolean defaultPolicy,
        List<RefundTierDto> tiers,
        Instant createdAt
) {
    public static RefundPolicyResponse from(RefundPolicy policy) {
        List<RefundTierDto> tiers = policy.getTiers().stream()
                .map(t -> new RefundTierDto(t.getHoursBeforeShow(), t.getRefundPercent()))
                .toList();
        return new RefundPolicyResponse(policy.getId(), policy.getName(),
                policy.isDefaultPolicy(), tiers, policy.getCreatedAt());
    }
}
