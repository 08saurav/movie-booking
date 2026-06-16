package com.example.booking.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * {@code premiumRows} names which row labels (e.g. {@code "D"}, {@code "E"})
 * should be generated as {@code PREMIUM} seats; every other row is
 * {@code REGULAR}. Omit or leave empty for an all-regular screen. Row labels
 * are validated against {@code totalRows} in the service layer (a
 * cross-field rule Bean Validation can't express directly).
 */
public record ScreenRequest(
        @NotNull Long theaterId,
        @NotBlank @Size(max = 100) String name,
        @Min(1) @Max(26) int totalRows,
        @Min(1) @Max(100) int totalCols,
        Set<String> premiumRows
) {
}
