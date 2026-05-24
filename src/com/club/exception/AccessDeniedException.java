package com.club.exception;

/**
 * Thrown when a user attempts to access a resource without sufficient permissions.
 * This exception enforces the RBAC security model by blocking unauthorized operations.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class AccessDeniedException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String requiredPermission;
    private final String attemptedAction;

    public AccessDeniedException(String username, String requiredPermission, String attemptedAction) {
        super("ERR_ACCESS_DENIED",
            String.format("User '%s' lacks permission '%s' required for action '%s'",
                username, requiredPermission, attemptedAction));
        this.username = username;
        this.requiredPermission = requiredPermission;
        this.attemptedAction = attemptedAction;
    }

    public AccessDeniedException(String message) {
        super("ERR_ACCESS_DENIED", message);
        this.username = "UNKNOWN";
        this.requiredPermission = "UNKNOWN";
        this.attemptedAction = "UNKNOWN";
    }

    public AccessDeniedException(String username, String message) {
        super("ERR_ACCESS_DENIED", String.format("User '%s': %s", username, message));
        this.username = username;
        this.requiredPermission = "UNKNOWN";
        this.attemptedAction = "UNKNOWN";
    }

    public String getUsername() {
        return username;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public String getAttemptedAction() {
        return attemptedAction;
    }
}
