package com.club.exception;

/**
 * Thrown when an invalid or expired JWT/session token is presented.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class InvalidTokenException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String tokenPreview;

    public InvalidTokenException(String tokenPreview, String reason) {
        super("ERR_INVALID_TOKEN", String.format("Invalid token '%s...': %s", tokenPreview, reason));
        this.tokenPreview = tokenPreview;
    }

    public InvalidTokenException(String message) {
        super("ERR_INVALID_TOKEN", message);
        this.tokenPreview = "UNKNOWN";
    }

    public String getTokenPreview() {
        return tokenPreview;
    }
}
