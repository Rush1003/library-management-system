package com.library.lms.exception;

/**
 * Thrown when a requested Book, Member, or Loan cannot be found by id.
 * Mapped to HTTP 404 (Not Found) by the GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException forId(String resource, String id) {
        return new ResourceNotFoundException(resource + " not found with id: " + id);
    }
}
