package com.club.exception;

/**
 * Thrown when a booking exceeds the maximum allowed tickets per transaction.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class ExceedsMaxTicketsException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final int requested;
    private final int maximum;

    public ExceedsMaxTicketsException(int requested, int maximum) {
        super("ERR_EXCEEDS_MAX_TICKETS",
            String.format("Requested %d tickets exceeds maximum allowed of %d per transaction", requested, maximum));
        this.requested = requested;
        this.maximum = maximum;
    }

    public int getRequested() {
        return requested;
    }

    public int getMaximum() {
        return maximum;
    }
}
