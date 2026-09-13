package com.library.lms.exception;

/**
 * Thrown when a member tries to borrow a book that currently has zero
 * available copies. Mapped to HTTP 409 (Conflict).
 */
public class BookNotAvailableException extends RuntimeException {
    public BookNotAvailableException(String message) {
        super(message);
    }
}
