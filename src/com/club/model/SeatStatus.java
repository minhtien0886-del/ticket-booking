package com.club.model;

/**
 * Enumeration of all possible states for a stadium seat.
 * Implements a state machine: AVAILABLE -> LOCKED -> BOOKED.
 * Seats can transition back to AVAILABLE only through cancellation.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public enum SeatStatus {

    /** Seat is available for booking. */
    AVAILABLE("Available for booking", true),

    /** Seat is temporarily locked during transaction processing. */
    LOCKED("Temporarily locked during checkout", false),

    /** Seat has been successfully booked and paid. */
    BOOKED("Booked and confirmed", false),

    /** Seat is reserved for VIP or season ticket holders. */
    RESERVED("Reserved for VIP/Season holders", false),

    /** Seat is unavailable due to maintenance or damage. */
    UNAVAILABLE("Unavailable — maintenance or damage", false);

    private final String description;
    private final boolean bookable;

    SeatStatus(String description, boolean bookable) {
        this.description = description;
        this.bookable = bookable;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBookable() {
        return bookable;
    }

    /**
     * Validates whether a state transition is legal per the seat state machine.
     *
     * @param from the current state
     * @param to   the target state
     * @return true if the transition is valid, false otherwise
     */
    public static boolean isValidTransition(SeatStatus from, SeatStatus to) {
        if (from == null) {
            return to == AVAILABLE;
        }
        switch (from) {
            case AVAILABLE:
                return to == LOCKED || to == RESERVED || to == UNAVAILABLE;
            case LOCKED:
                return to == BOOKED || to == AVAILABLE;
            case BOOKED:
                return to == AVAILABLE;
            case RESERVED:
                return to == BOOKED || to == UNAVAILABLE;
            case UNAVAILABLE:
                return to == AVAILABLE;
            default:
                return false;
        }
    }
}
