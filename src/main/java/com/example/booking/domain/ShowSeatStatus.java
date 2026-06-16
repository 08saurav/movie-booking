package com.example.booking.domain;

/**
 * Lifecycle of a single seat for a single show. Only AVAILABLE is reachable
 * in Segment 2 (every seed/generated show_seat starts here). HELD, BOOKED,
 * and CANCELLED are driven by the hold/booking/cancellation flows added in
 * Segments 3-5.
 */
public enum ShowSeatStatus {
    AVAILABLE,
    HELD,
    BOOKED,
    CANCELLED
}
