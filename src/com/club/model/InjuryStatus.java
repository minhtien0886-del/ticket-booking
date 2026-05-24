package com.club.model;

/**
 * Enumeration representing the injury status of a player in the FCM-ERP system.
 * Tracks the physical condition of players for team selection and fitness reporting.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public enum InjuryStatus {

    /** Player is fully fit and available for selection. */
    HEALTHY("Player is fully fit and available", 0),

    /** Player has a minor injury and is day-to-day. */
    MINOR_INJURY("Minor injury — day-to-day", 7),

    /** Player has a moderate injury with expected recovery time. */
    MODERATE_INJURY("Moderate injury — several weeks recovery", 21),

    /** Player has a serious injury requiring extended rehabilitation. */
    SERIOUS_INJURY("Serious injury — months of rehabilitation", 90),

    /** Player has undergone surgery and is in post-operative recovery. */
    POST_SURGERY("Post-surgery — on rehabilitation program", 120),

    /** Player's fitness status is under medical review. */
    UNDER_REVIEW("Fitness status under medical review", -1);

    private final String description;
    private final int estimatedRecoveryDays;

    InjuryStatus(String description, int estimatedRecoveryDays) {
        this.description = description;
        this.estimatedRecoveryDays = estimatedRecoveryDays;
    }

    public String getDescription() {
        return description;
    }

    public int getEstimatedRecoveryDays() {
        return estimatedRecoveryDays;
    }

    /**
     * Checks whether the player is available for team selection.
     *
     * @return true if the player can be selected, false otherwise
     */
    public boolean isAvailable() {
        return this == HEALTHY;
    }

    /**
     * Checks whether the injury is career-threatening (severe).
     *
     * @return true if the injury is severe, false otherwise
     */
    public boolean isCareerThreatening() {
        return this == SERIOUS_INJURY || this == POST_SURGERY;
    }
}
