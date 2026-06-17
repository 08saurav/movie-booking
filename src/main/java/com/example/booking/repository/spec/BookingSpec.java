package com.example.booking.repository.spec;

import com.example.booking.domain.Booking;
import com.example.booking.domain.BookingStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class BookingSpec {

    public static Specification<Booking> customerEq(String customer) {
        return (root, q, cb) -> customer == null ? null : cb.equal(root.get("customer"), customer);
    }

    public static Specification<Booking> statusEq(BookingStatus status) {
        return (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Booking> showIdEq(Long showId) {
        return (root, q, cb) -> showId == null ? null : cb.equal(root.get("show").get("id"), showId);
    }

    /** Bookings whose createdAt is on or after the start of this date (UTC). */
    public static Specification<Booking> fromDate(LocalDate from) {
        return (root, q, cb) -> {
            if (from == null) return null;
            Instant instant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
            return cb.greaterThanOrEqualTo(root.get("createdAt"), instant);
        };
    }

    /** Bookings whose createdAt is before the end of this date (UTC). */
    public static Specification<Booking> toDate(LocalDate to) {
        return (root, q, cb) -> {
            if (to == null) return null;
            Instant instant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            return cb.lessThan(root.get("createdAt"), instant);
        };
    }
}
