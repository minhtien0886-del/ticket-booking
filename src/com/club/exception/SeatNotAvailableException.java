package com.club.exception;

/**
 * Thrown when a seat is not available for booking.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class SeatNotAvailableException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String seatId;
    private final String currentStatus;

    public SeatNotAvailableException(String seatId, String currentStatus) {
        super("ERR_SEAT_NOT_AVAILABLE",
            String.format("Seat '%s' is not available for booking. Current status: %s", seatId, currentStatus));
        this.seatId = seatId;
        this.currentStatus = currentStatus;
    }

    public SeatNotAvailableException(String message) {
        super("ERR_SEAT_NOT_AVAILABLE", message);
        this.seatId = "UNKNOWN";
        this.currentStatus = "UNKNOWN";
    }

    public String getSeatId() {
        return seatId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}
