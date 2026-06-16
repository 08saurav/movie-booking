package com.example.booking.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Smoke-test endpoints used to verify the full stack (web + security + context)
 * is alive and that role-based access control works end-to-end. These are
 * scaffolding for Segment 1 and will be removed or superseded by real endpoints
 * in later segments.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Connectivity and security smoke-test endpoints")
public class PingController {

    @GetMapping("/ping")
    @Operation(summary = "Authenticated ping",
            description = "Returns 200 for any authenticated user; 401 when anonymous.")
    public Map<String, Object> ping(Authentication authentication) {
        return Map.of(
                "message", "pong",
                "timestamp", Instant.now().toString(),
                "user", authentication.getName(),
                "roles", authentication.getAuthorities().stream().map(Object::toString).toList()
        );
    }

    @GetMapping("/admin/ping")
    @Operation(summary = "Admin-only ping",
            description = "Returns 200 only for ROLE_ADMIN; 403 for an authenticated non-admin.")
    public Map<String, Object> adminPing(Authentication authentication) {
        return Map.of(
                "message", "admin pong",
                "timestamp", Instant.now().toString(),
                "user", authentication.getName()
        );
    }
}
