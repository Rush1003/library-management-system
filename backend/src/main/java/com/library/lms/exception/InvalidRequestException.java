package com.library.lms.exception;

/**
 * Thrown when a request is well-formed JSON but violates a business rule
 * (e.g. trying to return a loan that was already returned).
 * Mapped to HTTP 400 (Bad Request).
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
