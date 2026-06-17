package com.example.booking.web;

import com.example.booking.domain.SeatCategory;
import com.example.booking.domain.ShowSeatStatus;
import com.example.booking.service.CityService;
import com.example.booking.service.ShowService;
import com.example.booking.service.TheaterService;
import com.example.booking.web.dto.CityResponse;
import com.example.booking.web.dto.ShowResponse;
import com.example.booking.web.dto.ShowSeatResponse;
import com.example.booking.web.dto.TheaterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only endpoints any authenticated user (customer or admin) can call to
 * browse the catalog. Requires authentication only -- not ROLE_ADMIN -- per
 * {@code SecurityConfig}'s "anything outside /api/admin/** just needs to be
 * authenticated" rule.
 */
@RestController
@Tag(name = "Customer: Browse", description = "Browse cities, theaters, shows, and seat availability")
public class BrowseController {

    private final CityService cityService;
    private final TheaterService theaterService;
    private final ShowService showService;

    public BrowseController(CityService cityService, TheaterService theaterService, ShowService showService) {
        this.cityService = cityService;
        this.theaterService = theaterService;
        this.showService = showService;
    }

    @GetMapping("/api/cities")
    @Operation(summary = "List all cities",
            description = "Optional filters: name (partial), state, country. All combinable.")
    public List<CityResponse> listCities(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String country) {
        return cityService.listAll(name, state, country);
    }

    @GetMapping("/api/cities/{id}/theaters")
    @Operation(summary = "List theaters in a city",
            description = "Optional filter: name (partial).")
    public List<TheaterResponse> listTheatersInCity(
            @PathVariable Long id,
            @RequestParam(required = false) String name) {
        return theaterService.listByCity(id, name);
    }

    @GetMapping("/api/theaters/{id}/shows")
    @Operation(summary = "List upcoming shows at a theater",
            description = "Soonest first; past shows are excluded. Optional filters: movieId, movieTitle (partial), "
                    + "date (YYYY-MM-DD), language, genre.")
    public List<ShowResponse> listUpcomingShowsAtTheater(
            @PathVariable Long id,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String movieTitle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String genre) {
        return showService.listUpcomingForTheater(id, movieId, movieTitle, date, language, genre);
    }

    @GetMapping("/api/shows/{id}/seats")
    @Operation(summary = "List all seats for a show with real-time status",
            description = "Returns seats with their current status and effective price. "
                    + "The 'heldByMe' field is true only for seats the requesting customer currently holds. "
                    + "Optional filters: status (AVAILABLE/HELD/BOOKED/CANCELLED), category (REGULAR/PREMIUM), rowLabel (e.g. A, B).")
    public List<ShowSeatResponse> listSeatsWithStatus(
            @PathVariable Long id,
            @RequestParam(required = false) ShowSeatStatus status,
            @RequestParam(required = false) SeatCategory category,
            @RequestParam(required = false) String rowLabel,
            Authentication authentication) {
        String currentUser = authentication != null ? authentication.getName() : null;
        return showService.getSeatsWithStatus(id, status, category, rowLabel, currentUser);
    }
}
