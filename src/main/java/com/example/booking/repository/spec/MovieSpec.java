package com.example.booking.repository.spec;

import com.example.booking.domain.Movie;
import org.springframework.data.jpa.domain.Specification;

public class MovieSpec {

    public static Specification<Movie> titleLike(String title) {
        return (root, q, cb) -> (title == null || title.isBlank()) ? null
                : cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Movie> languageEq(String language) {
        return (root, q, cb) -> language == null ? null : cb.equal(root.get("language"), language);
    }

    public static Specification<Movie> genreEq(String genre) {
        return (root, q, cb) -> genre == null ? null : cb.equal(root.get("genre"), genre);
    }

    public static Specification<Movie> ratingEq(String rating) {
        return (root, q, cb) -> rating == null ? null : cb.equal(root.get("rating"), rating);
    }
}
