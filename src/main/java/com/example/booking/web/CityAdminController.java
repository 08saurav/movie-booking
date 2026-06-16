package com.example.booking.web;

import com.example.booking.service.CityService;
import com.example.booking.web.dto.CityRequest;
import com.example.booking.web.dto.CityResponse;
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
@RequestMapping("/api/admin/cities")
@Tag(name = "Admin: Cities", description = "Manage cities (ROLE_ADMIN)")
public class CityAdminController {

    private final CityService cityService;

    public CityAdminController(CityService cityService) {
        this.cityService = cityService;
    }

    @PostMapping
    @Operation(summary = "Create a city")
    public ResponseEntity<CityResponse> create(@Valid @RequestBody CityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a city")
    public CityResponse update(@PathVariable Long id, @Valid @RequestBody CityRequest request) {
        return cityService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a city",
            description = "Fails with 409 if the city still has theaters that have scheduled shows.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a city by id")
    public CityResponse getById(@PathVariable Long id) {
        return cityService.getById(id);
    }

    @GetMapping
    @Operation(summary = "List all cities")
    public List<CityResponse> listAll() {
        return cityService.listAll();
    }
}
