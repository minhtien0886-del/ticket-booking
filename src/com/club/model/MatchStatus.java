package com.club.model;

/**
 * Enumeration of match statuses throughout the match lifecycle.
 * Models the complete state machine for a football match from scheduling
 * through completion and record archival.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public enum MatchStatus {

    SCHEDULED("Match is scheduled for future date/time", true, false),
    TICKETS_ON_SALE("Tickets are available for purchase", true, false),
    NEARLY_SOLD_OUT("Ticket inventory below 20%", true, false),
    SOLD_OUT("All tickets have been sold", false, false),
    IN_PROGRESS("Match is currently being played", false, false),
    HALF_TIME("Match is in halftime interval", false, false),
    POSTPONED("Match has been postponed", false, true),
    CANCELLED("Match has been cancelled", false, true),
    COMPLETED("Match has been completed", false, true);

    private final String description;
    private final boolean ticketsAvailable;
    private final boolean finalState;

    MatchStatus(String description, boolean ticketsAvailable, boolean finalState) {
        this.description = description;
        this.ticketsAvailable = ticketsAvailable;
        this.finalState = finalState;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTicketsAvailable() {
        return ticketsAvailable;
    }

    public boolean isFinalState() {
        return finalState;
    }

    /**
     * Checks whether tickets can still be purchased for this match.
     *
     * @return true if tickets are available for sale
     */
    public boolean canBookTickets() {
        return this == TICKETS_ON_SALE || this == SCHEDULED;
    }

    /**
     * Checks whether the match result is finalized.
     *
     * @return true if the match is over and results are final
     */
    public boolean isResultFinal() {
        return this == COMPLETED || this == CANCELLED || this == POSTPONED;
    }
}
