package com.club.model;

/**
 * Enumeration of ticket categories for match attendance.
 * Each category has a distinct pricing tier and access level.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public enum TicketCategory {

    VIP("VIP Box Seat", 1.0),
    PREMIUM("Premium Seat", 0.75),
    STANDARD("Standard Seat", 0.50),
    ECONOMY("Economy Seat", 0.25),
    STUDENT("Student Ticket", 0.15),
    CHILD("Child Ticket (under 12)", 0.10),
    SENIOR("Senior Citizen Ticket (65+)", 0.10);

    private final String displayName;
    private final double priceMultiplier;

    TicketCategory(String displayName, double priceMultiplier) {
        this.displayName = displayName;
        this.priceMultiplier = priceMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getPriceMultiplier() {
        return priceMultiplier;
    }

    /**
     * Calculates the ticket price given a base price.
     *
     * @param basePrice the base price for a standard ticket
     * @return the calculated price for this category
     */
    public double calculatePrice(double basePrice) {
        return basePrice * priceMultiplier;
    }
}
