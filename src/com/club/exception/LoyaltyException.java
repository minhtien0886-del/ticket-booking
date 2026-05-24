package com.club.exception;

/**
 * Thrown when a loyalty points operation (earn/redeem) fails.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class LoyaltyException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String fanId;
    private final int points;

    public LoyaltyException(String fanId, int points, String reason) {
        super("ERR_LOYALTY",
            String.format("Loyalty error for fan '%s' (points=%d): %s", fanId, points, reason));
        this.fanId = fanId;
        this.points = points;
    }

    public LoyaltyException(String message) {
        super("ERR_LOYALTY", message);
        this.fanId = "UNKNOWN";
        this.points = 0;
    }

    public String getFanId() {
        return fanId;
    }

    public int getPoints() {
        return points;
    }
}
