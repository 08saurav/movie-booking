package com.example.booking.exception;

import java.time.Instant;

/** Consistent error shape returned by every endpoint via {@link GlobalExceptionHandler}. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
