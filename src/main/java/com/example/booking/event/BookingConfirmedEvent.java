package com.example.booking.event;

import java.math.BigDecimal;

/**
 * Published (within the booking transaction) after a booking reaches CONFIRMED state.
 * Carries all data needed for the notification as primitives — no JPA entity
 * references — because the listener runs on an async thread with no transaction context.
 */
public record BookingConfirmedEvent(
        Long bookingId,
        String customer,
        String movieTitle,
        String theaterName,
        String seatLabel,
        BigDecimal finalPrice
) {}
