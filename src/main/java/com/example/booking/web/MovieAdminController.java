package com.example.booking.web;

import com.example.booking.service.MovieService;
import com.example.booking.web.dto.MovieRequest;
import com.example.booking.web.dto.MovieResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/movies")
@Tag(name = "Admin: Movies", description = "Manage the movie catalog (ROLE_ADMIN)")
public class MovieAdminController {

    private final MovieService movieService;

    public MovieAdminController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    @Operation(summary = "Create a movie")
    public ResponseEntity<MovieResponse> create(@Valid @RequestBody MovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a movie")
    public MovieResponse update(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        return movieService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a movie", description = "Fails with 409 if the movie still has scheduled shows.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        movieService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a movie by id")
    public MovieResponse getById(@PathVariable Long id) {
        return movieService.getById(id);
    }

    @GetMapping
    @Operation(summary = "List all movies")
    public List<MovieResponse> listAll() {
        return movieService.listAll();
    }
}
