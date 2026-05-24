package com.club.exception;

/**
 * Thrown when authentication fails due to invalid credentials or an inactive account.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class AuthenticationException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String username;

    public AuthenticationException(String username, String reason) {
        super("ERR_AUTH_FAILED",
            String.format("Authentication failed for user '%s': %s", username, reason));
        this.username = username;
    }

    public AuthenticationException(String message) {
        super("ERR_AUTH_FAILED", message);
        this.username = "UNKNOWN";
    }

    public String getUsername() {
        return username;
    }
}
