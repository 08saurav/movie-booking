package com.example.booking.repository.spec;

import com.example.booking.domain.Show;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class ShowSpec {

    public static Specification<Show> movieIdEq(Long movieId) {
        return (root, q, cb) -> movieId == null ? null : cb.equal(root.get("movie").get("id"), movieId);
    }

    public static Specification<Show> movieTitleLike(String title) {
        return (root, q, cb) -> (title == null || title.isBlank()) ? null
                : cb.like(cb.lower(root.get("movie").get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Show> screenIdEq(Long screenId) {
        return (root, q, cb) -> screenId == null ? null : cb.equal(root.get("screen").get("id"), screenId);
    }

    public static Specification<Show> theaterIdEq(Long theaterId) {
        return (root, q, cb) -> theaterId == null ? null
                : cb.equal(root.get("screen").get("theater").get("id"), theaterId);
    }

    public static Specification<Show> cityIdEq(Long cityId) {
        return (root, q, cb) -> cityId == null ? null
                : cb.equal(root.get("screen").get("theater").get("city").get("id"), cityId);
    }

    /** Matches shows whose startTime falls on this calendar date (UTC). */
    public static Specification<Show> onDate(LocalDate date) {
        return (root, q, cb) -> {
            if (date == null) return null;
            Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            return cb.and(
                    cb.greaterThanOrEqualTo(root.get("startTime"), start),
                    cb.lessThan(root.get("startTime"), end));
        };
    }

    public static Specification<Show> startFrom(Instant from) {
        return (root, q, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("startTime"), from);
    }

    public static Specification<Show> startBefore(Instant to) {
        return (root, q, cb) -> to == null ? null : cb.lessThan(root.get("startTime"), to);
    }

    public static Specification<Show> startAfter(Instant after) {
        return (root, q, cb) -> after == null ? null : cb.greaterThan(root.get("startTime"), after);
    }

    public static Specification<Show> languageEq(String language) {
        return (root, q, cb) -> language == null ? null : cb.equal(root.get("movie").get("language"), language);
    }

    public static Specification<Show> genreEq(String genre) {
        return (root, q, cb) -> genre == null ? null : cb.equal(root.get("movie").get("genre"), genre);
    }
}
