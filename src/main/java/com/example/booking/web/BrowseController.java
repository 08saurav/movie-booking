package com.example.booking.web;

import com.example.booking.service.CityService;
import com.example.booking.service.ShowService;
import com.example.booking.service.TheaterService;
import com.example.booking.web.dto.CityResponse;
import com.example.booking.web.dto.ShowResponse;
import com.example.booking.web.dto.ShowSeatResponse;
import com.example.booking.web.dto.TheaterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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
    @Operation(summary = "List all cities")
    public List<CityResponse> listCities() {
        return cityService.listAll();
    }

    @GetMapping("/api/cities/{id}/theaters")
    @Operation(summary = "List theaters in a city")
    public List<TheaterResponse> listTheatersInCity(@PathVariable Long id) {
        return theaterService.listByCity(id);
    }

    @GetMapping("/api/theaters/{id}/shows")
    @Operation(summary = "List upcoming shows at a theater", description = "Soonest first; past shows are excluded.")
    public List<ShowResponse> listUpcomingShowsAtTheater(@PathVariable Long id) {
        return showService.listUpcomingForTheater(id);
    }

    @GetMapping("/api/shows/{id}/seats")
    @Operation(summary = "List all seats for a show with real-time status",
            description = "Returns all seats with their current status: AVAILABLE, HELD (time-bound), BOOKED, or CANCELLED.")
    public List<ShowSeatResponse> listSeatsWithStatus(@PathVariable Long id) {
        return showService.getSeatsWithStatus(id);
    }
}
