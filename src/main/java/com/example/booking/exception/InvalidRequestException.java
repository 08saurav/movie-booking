package com.example.booking.exception;

/**
 * For request-level validation that Bean Validation annotations can't
 * express (cross-field rules, e.g. a screen's {@code premiumRows} containing
 * a row label outside that screen's row range). Maps to 400, like a Bean
 * Validation failure.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
