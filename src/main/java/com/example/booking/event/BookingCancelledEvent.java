package com.example.booking.event;

import java.math.BigDecimal;

/**
 * Published (within the cancellation transaction) after a booking reaches CANCELLED state.
 * Carries all data as primitives — no JPA entity references — for async delivery.
 */
public record BookingCancelledEvent(
        Long bookingId,
        String customer,
        String movieTitle,
        String theaterName,
        String seatLabel,
        BigDecimal refundAmount
) {}
