package com.example.booking.web.dto;

import com.example.booking.domain.Booking;
import com.example.booking.domain.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingResponse(
        Long id,
        String customer,
        Long showId,
        String movieTitle,
        Long showSeatId,
        String seatLabel,
        String seatCategory,
        Long pricingTierId,
        String pricingTierName,
        String discountCode,
        BigDecimal basePrice,
        BigDecimal discountAmount,
        BigDecimal finalPrice,
        BookingStatus status,
        String idempotencyKey,
        Instant createdAt,
        Instant confirmedAt
) {
    public static BookingResponse from(Booking booking) {
        var seat = booking.getShowSeat().getSeat();
        var dc = booking.getDiscountCode();
        return new BookingResponse(
                booking.getId(),
                booking.getCustomer(),
                booking.getShow().getId(),
                booking.getShow().getMovie().getTitle(),
                booking.getShowSeat().getId(),
                seat.getRowLabel() + seat.getSeatNumber(),
                seat.getCategory().name(),
                booking.getPricingTier().getId(),
                booking.getPricingTier().getName(),
                dc != null ? dc.getCode() : null,
                booking.getBasePrice(),
                booking.getDiscountAmount(),
                booking.getFinalPrice(),
                booking.getStatus(),
                booking.getIdempotencyKey(),
                booking.getCreatedAt(),
                booking.getConfirmedAt());
    }
}
