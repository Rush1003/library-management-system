package com.library.lms.exception;

/**
 * Thrown when attempting to create a resource that violates a uniqueness
 * constraint (e.g. an ISBN or member email that already exists).
 * Mapped to HTTP 409 (Conflict) by the GlobalExceptionHandler.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
