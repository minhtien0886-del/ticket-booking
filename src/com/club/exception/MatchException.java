package com.club.exception;

/**
 * Thrown when a match-related operation fails.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class MatchException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String matchId;

    public MatchException(String matchId, String reason) {
        super("ERR_MATCH", String.format("Match '%s': %s", matchId, reason));
        this.matchId = matchId;
    }

    public MatchException(String message) {
        super("ERR_MATCH", message);
        this.matchId = "UNKNOWN";
    }

    public String getMatchId() {
        return matchId;
    }
}
