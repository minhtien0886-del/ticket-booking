package com.club.exception;

/**
 * Thrown when a session-related operation fails, such as session expiration or invalid session data.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class SessionException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String sessionId;

    public SessionException(String sessionId, String reason) {
        super("ERR_SESSION", String.format("Session '%s': %s", sessionId, reason));
        this.sessionId = sessionId;
    }

    public SessionException(String message) {
        super("ERR_SESSION", message);
        this.sessionId = "UNKNOWN";
    }

    public String getSessionId() {
        return sessionId;
    }
}
