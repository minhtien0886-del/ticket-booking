package com.club.model;

/**
 * Enumeration of product categories for merchandise items in the club shop.
 * Each category has a distinct tax rate applied during checkout.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public enum ProductCategory {

    JERSEY("Official Club Jersey", 0.08),
    SCARF("Club Scarf and Accessories", 0.05),
    CAP("Club Cap and Headwear", 0.05),
    JACKET("Club Jacket and Outerwear", 0.08),
    TICKET_PACKAGE("Season Ticket Package", 0.0),
    FOOTBALL("Official Match Ball", 0.05),
    FLAG("Club Flag and Banner", 0.05),
    MUG("Club Mug and Drinkware", 0.05),
    KEYCHAIN("Club Keychain and Souvenirs", 0.05),
    TRAINING_GEAR("Training Equipment", 0.08),
    /** Apparel: official jerseys, tracksuits, training wear. */
    KIT("Club Apparel and Kits", 0.08),
    /** Footwear: football boots and lifestyle shoes. */
    FOOTWEAR("Football Boots and Footwear", 0.08),
    /** Accessories: socks, caps, scarves, merchandise add-ons. */
    ACCESSORIES("Club Accessories", 0.05);

    private final String displayName;
    private final double taxRate;

    ProductCategory(String displayName, double taxRate) {
        this.displayName = displayName;
        this.taxRate = taxRate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getTaxRate() {
        return taxRate;
    }

    /**
     * Calculates the total price including tax.
     *
     * @param basePrice the base price of the product
     * @return price including tax
     */
    public double priceWithTax(double basePrice) {
        return basePrice * (1.0 + taxRate);
    }
}
