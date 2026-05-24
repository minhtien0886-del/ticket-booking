package com.club.model;

/**
 * Enumeration of fan loyalty tiers within the club's rewards program.
 * Higher tiers unlock better discounts, priority access, and exclusive perks.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public enum LoyaltyTier {

    BRONZE("Bronze Fan", 0, 0.0, 1),
    SILVER("Silver Fan", 1000, 0.05, 2),
    GOLD("Gold Fan", 5000, 0.10, 3),
    PLATINUM("Platinum Fan", 15000, 0.15, 4),
    DIAMOND("Diamond Fan", 50000, 0.20, 5);

    private final String displayName;
    private final int minPoints;
    private final double discountRate;
    private final int priorityLevel;

    LoyaltyTier(String displayName, int minPoints, double discountRate, int priorityLevel) {
        this.displayName = displayName;
        this.minPoints = minPoints;
        this.discountRate = discountRate;
        this.priorityLevel = priorityLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinPoints() {
        return minPoints;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }

    /**
     * Determines the appropriate tier for a given loyalty point balance.
     *
     * @param points the fan's current loyalty points
     * @return the corresponding LoyaltyTier
     */
    public static LoyaltyTier fromPoints(int points) {
        if (points >= DIAMOND.minPoints)    return DIAMOND;
        if (points >= PLATINUM.minPoints)    return PLATINUM;
        if (points >= GOLD.minPoints)        return GOLD;
        if (points >= SILVER.minPoints)     return SILVER;
        return BRONZE;
    }

    /**
     * Calculates the ticket price after applying this tier's discount.
     *
     * @param originalPrice the original ticket price
     * @return the discounted price
     */
    public double applyDiscount(double originalPrice) {
        return originalPrice * (1.0 - discountRate);
    }

    /**
     * Checks whether this tier has higher or equal priority than another.
     *
     * @param other the other tier to compare
     * @return true if this tier is >= other tier
     */
    public boolean isAtLeast(LoyaltyTier other) {
        return this.priorityLevel >= other.priorityLevel;
    }
}
