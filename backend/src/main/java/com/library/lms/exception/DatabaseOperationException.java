package com.library.lms.exception;

/**
 * Wraps any low-level database failure (e.g. MongoDB connection lost,
 * timeout, cluster unreachable) that the service layer catches.
 *
 * This is part of the application's fallback strategy: rather than letting
 * a raw MongoDB/driver exception (with a stack trace and internal details)
 * escape to the client, the service layer catches it, logs the root cause,
 * and re-throws this exception so the client always receives a clean,
 * predictable JSON error response (HTTP 503 Service Unavailable).
 */
public class DatabaseOperationException extends RuntimeException {
    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
