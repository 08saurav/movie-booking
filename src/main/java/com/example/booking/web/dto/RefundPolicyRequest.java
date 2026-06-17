package com.example.booking.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RefundPolicyRequest(
        @NotBlank String name,
        boolean defaultPolicy,
        @NotEmpty @Valid List<RefundTierDto> tiers
) {}
