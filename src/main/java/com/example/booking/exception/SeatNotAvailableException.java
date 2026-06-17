package com.example.booking.exception;

/**
 * Raised when a seat cannot be held or booked due to unavailability.
 * Returns HTTP 409 Conflict.
 */
public class SeatNotAvailableException extends RuntimeException {

    public SeatNotAvailableException(String message) {
        super(message);
    }

    public SeatNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
