package com.example.booking.web;

import com.example.booking.service.ScreenService;
import com.example.booking.web.dto.ScreenRenameRequest;
import com.example.booking.web.dto.ScreenRequest;
import com.example.booking.web.dto.ScreenResponse;
import com.example.booking.web.dto.SeatResponse;
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
@RequestMapping("/api/admin/screens")
@Tag(name = "Admin: Screens", description = "Manage screens and their generated seat layout (ROLE_ADMIN)")
public class ScreenAdminController {

    private final ScreenService screenService;

    public ScreenAdminController(ScreenService screenService) {
        this.screenService = screenService;
    }

    @PostMapping
    @Operation(summary = "Create a screen",
            description = "Generates the seat layout (totalRows x totalCols) automatically. "
                    + "Rows listed in premiumRows are generated as PREMIUM; all others are REGULAR.")
    public ResponseEntity<ScreenResponse> create(@Valid @RequestBody ScreenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(screenService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename a screen",
            description = "Only the name can be changed; the seat layout is fixed at creation time.")
    public ScreenResponse rename(@PathVariable Long id, @Valid @RequestBody ScreenRenameRequest request) {
        return screenService.rename(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a screen",
            description = "Fails with 409 if the screen still has scheduled shows.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        screenService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a screen by id")
    public ScreenResponse getById(@PathVariable Long id) {
        return screenService.getById(id);
    }

    @GetMapping
    @Operation(summary = "List screens",
            description = "Optional filters: theaterId, name (partial). All combinable.")
    public List<ScreenResponse> listAll(
            @RequestParam(required = false) Long theaterId,
            @RequestParam(required = false) String name) {
        return screenService.listAll(theaterId, name);
    }

    @GetMapping("/{id}/seats")
    @Operation(summary = "List a screen's generated seat layout",
            description = "Verification helper: confirms the seats generated at screen-creation time.")
    public List<SeatResponse> getSeats(@PathVariable Long id) {
        return screenService.getSeats(id);
    }
}
