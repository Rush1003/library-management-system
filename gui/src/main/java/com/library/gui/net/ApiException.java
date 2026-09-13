package com.library.gui.net;

/**
 * Thrown by ApiClient for any failure calling the backend: a network
 * problem (server down, timeout) or a non-2xx HTTP response. Carries a
 * human-readable message safe to display directly in a JOptionPane.
 */
public class ApiException extends Exception {

    private final int statusCode; // 0 if it never reached the server (connection/timeout)

    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isConnectivityFailure() {
        return statusCode == 0;
    }
}
