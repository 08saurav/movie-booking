package com.example.booking.web.dto;

import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull Long showId,
        @NotNull Long showSeatId,
        String discountCode
) {}
