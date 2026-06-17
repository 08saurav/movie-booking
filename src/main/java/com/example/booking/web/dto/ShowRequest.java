package com.example.booking.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ShowRequest(
        @NotNull Long movieId,
        @NotNull Long screenId,
        @NotNull @Future Instant startTime,
        Long pricingTierId,
        Long refundPolicyId
) {
}
