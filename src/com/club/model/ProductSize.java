package com.club.model;

/**
 * Enumeration of product sizes for merchandise items.
 * Supports multiple size schemes for apparel and accessories.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public enum ProductSize {

    XS("Extra Small"),
    S("Small"),
    M("Medium"),
    L("Large"),
    XL("Extra Large"),
    XXL("Double Extra Large"),
    ONE_SIZE("One Size Fits All"),
    SHOES_38("Shoe Size 38"),
    SHOES_39("Shoe Size 39"),
    SHOES_40("Shoe Size 40"),
    SHOES_41("Shoe Size 41"),
    SHOES_42("Shoe Size 42"),
    SHOES_43("Shoe Size 43"),
    SHOES_44("Shoe Size 44");

    private final String displayName;

    ProductSize(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
