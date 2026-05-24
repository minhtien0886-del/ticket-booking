package com.club.exception;

/**
 * Thrown when a seat state transition violates the seat state machine rules.
 * Valid transitions: AVAILABLE -> LOCKED -> BOOKED -> AVAILABLE.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class InvalidSeatStateException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String seatId;
    private final String currentState;
    private final String attemptedState;

    public InvalidSeatStateException(String seatId, String currentState, String attemptedState) {
        super("ERR_INVALID_SEAT_STATE",
            String.format("Seat '%s' cannot transition from %s to %s",
                seatId, currentState, attemptedState));
        this.seatId = seatId;
        this.currentState = currentState;
        this.attemptedState = attemptedState;
    }

    public String getSeatId() {
        return seatId;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getAttemptedState() {
        return attemptedState;
    }
}
