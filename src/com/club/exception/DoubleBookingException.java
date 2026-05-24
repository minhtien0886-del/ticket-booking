package com.club.exception;

/**
 * Thrown when a double-booking attempt is detected on the same seat by concurrent transactions.
 * This is the primary race condition that the three locking mechanisms aim to prevent.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class DoubleBookingException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String seatId;
    private final String matchId;
    private final String threadName;
    private final long timestamp;

    public DoubleBookingException(String seatId, String matchId) {
        super("ERR_DOUBLE_BOOKING",
            String.format("Double booking detected on seat '%s' for match '%s'", seatId, matchId));
        this.seatId = seatId;
        this.matchId = matchId;
        this.threadName = Thread.currentThread().getName();
        this.timestamp = System.currentTimeMillis();
    }

    public DoubleBookingException(String seatId, String matchId, String message) {
        super("ERR_DOUBLE_BOOKING", message);
        this.seatId = seatId;
        this.matchId = matchId;
        this.threadName = Thread.currentThread().getName();
        this.timestamp = System.currentTimeMillis();
    }

    public String getSeatId() {
        return seatId;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getThreadName() {
        return threadName;
    }

    public long getDetectionTimestamp() {
        return timestamp;
    }
}
