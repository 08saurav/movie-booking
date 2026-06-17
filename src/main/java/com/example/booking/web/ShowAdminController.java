package com.example.booking.web;

import com.example.booking.service.ShowService;
import com.example.booking.web.dto.ShowRequest;
import com.example.booking.web.dto.ShowRescheduleRequest;
import com.example.booking.web.dto.ShowResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/shows")
@Tag(name = "Admin: Shows", description = "Schedule shows and generate their per-show seat inventory (ROLE_ADMIN)")
public class ShowAdminController {

    private final ShowService showService;

    public ShowAdminController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping
    @Operation(summary = "Schedule a show",
            description = "endTime is derived from the movie's duration. Generates one AVAILABLE "
                    + "show_seat row per seat on the chosen screen.")
    public ResponseEntity<ShowResponse> create(@Valid @RequestBody ShowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(showService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reschedule a show",
            description = "Only startTime can change; movie/screen are fixed once the seat inventory exists.")
    public ShowResponse reschedule(@PathVariable Long id, @Valid @RequestBody ShowRescheduleRequest request) {
        return showService.reschedule(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a show", description = "Also deletes its per-show seat inventory.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        showService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a show by id")
    public ShowResponse getById(@PathVariable Long id) {
        return showService.getById(id);
    }

    @GetMapping
    @Operation(summary = "List shows",
            description = "Optional filters: movieId, screenId, theaterId, cityId, "
                    + "date (YYYY-MM-DD — shows on that exact day), "
                    + "from/to (YYYY-MM-DD — show start window), language, genre. All combinable.")
    public List<ShowResponse> listAll(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long screenId,
            @RequestParam(required = false) Long theaterId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String genre) {
        return showService.listAll(movieId, screenId, theaterId, cityId, date, from, to, language, genre);
    }
}
