package com.example.booking.web;

import com.example.booking.service.TheaterService;
import com.example.booking.web.dto.TheaterRequest;
import com.example.booking.web.dto.TheaterResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/theaters")
@Tag(name = "Admin: Theaters", description = "Manage theaters (ROLE_ADMIN)")
public class TheaterAdminController {

    private final TheaterService theaterService;

    public TheaterAdminController(TheaterService theaterService) {
        this.theaterService = theaterService;
    }

    @PostMapping
    @Operation(summary = "Create a theater in a city")
    public ResponseEntity<TheaterResponse> create(@Valid @RequestBody TheaterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(theaterService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a theater")
    public TheaterResponse update(@PathVariable Long id, @Valid @RequestBody TheaterRequest request) {
        return theaterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a theater",
            description = "Fails with 409 if any of its screens still have scheduled shows.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        theaterService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a theater by id")
    public TheaterResponse getById(@PathVariable Long id) {
        return theaterService.getById(id);
    }

    @GetMapping
    @Operation(summary = "List theaters", description = "Optionally filter by cityId.")
    public List<TheaterResponse> listAll(@RequestParam(required = false) Long cityId) {
        return theaterService.listAll(cityId);
    }
}
