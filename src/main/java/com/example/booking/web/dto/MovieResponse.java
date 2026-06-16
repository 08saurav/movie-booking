package com.example.booking.web.dto;

import com.example.booking.domain.Movie;

public record MovieResponse(
        Long id,
        String title,
        int durationMinutes,
        String language,
        String genre,
        String rating,
        String description
) {

    public static MovieResponse from(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDurationMinutes(),
                movie.getLanguage(),
                movie.getGenre(),
                movie.getRating(),
                movie.getDescription());
    }
}
