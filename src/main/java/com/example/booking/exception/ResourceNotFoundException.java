package com.example.booking.exception;

/**
 * Thrown whenever an entity looked up by id does not exist -- whether that id
 * came from the request path (e.g. {@code GET /api/admin/cities/{id}}) or
 * from a reference inside a request body (e.g. a {@code cityId} on a
 * theater-creation request). Deliberately one exception type for both cases:
 * from the caller's perspective, "the id you gave me doesn't exist" is the
 * same problem either way, and maps to the same 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
