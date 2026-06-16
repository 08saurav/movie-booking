package com.example.booking.service;

import com.example.booking.domain.Movie;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.MovieRepository;
import com.example.booking.web.dto.MovieRequest;
import com.example.booking.web.dto.MovieResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public MovieResponse create(MovieRequest request) {
        Movie movie = new Movie(request.title(), request.durationMinutes(), request.language(),
                request.genre(), request.rating(), request.description());
        return MovieResponse.from(movieRepository.save(movie));
    }

    public MovieResponse update(Long id, MovieRequest request) {
        Movie movie = findMovieOrThrow(id);
        movie.setTitle(request.title());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setLanguage(request.language());
        movie.setGenre(request.genre());
        movie.setRating(request.rating());
        movie.setDescription(request.description());
        return MovieResponse.from(movie);
    }

    public void delete(Long id) {
        movieRepository.delete(findMovieOrThrow(id));
    }

    @Transactional(readOnly = true)
    public MovieResponse getById(Long id) {
        return MovieResponse.from(findMovieOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> listAll() {
        return movieRepository.findAll().stream().map(MovieResponse::from).toList();
    }

    private Movie findMovieOrThrow(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie " + id + " not found"));
    }
}
