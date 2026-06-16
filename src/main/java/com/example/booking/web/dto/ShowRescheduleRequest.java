package com.example.booking.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** Update payload for a show: only the start time can change (see {@code Show} for why). */
public record ShowRescheduleRequest(@NotNull @Future Instant startTime) {
}
