package com.club.exception;

/**
 * Thrown when a match status transition is invalid.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class InvalidMatchStateException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String matchId;
    private final String currentState;
    private final String attemptedState;

    public InvalidMatchStateException(String matchId, String currentState, String attemptedState) {
        super("ERR_INVALID_MATCH_STATE",
            String.format("Match '%s' cannot transition from %s to %s", matchId, currentState, attemptedState));
        this.matchId = matchId;
        this.currentState = currentState;
        this.attemptedState = attemptedState;
    }

    public InvalidMatchStateException(String message) {
        super("ERR_INVALID_MATCH_STATE", message);
        this.matchId = "UNKNOWN";
        this.currentState = "UNKNOWN";
        this.attemptedState = "UNKNOWN";
    }

    public String getMatchId() {
        return matchId;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getAttemptedState() {
        return attemptedState;
    }
}
