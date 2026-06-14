package com.club.model;

import java.util.Objects;

/**
 * Represents a merchandise product in the club's online shop.
 * Supports size and color variants, stock tracking, and category-based pricing.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class Merchandise extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String productId;
    private String name;
    private String description;
    private ProductCategory category;
    private double basePrice;
    private String size;
    private String color;
    private int stockQuantity;
    private boolean active;
    private String imageUrl;

    public Merchandise() {
        this.active = true;
        this.stockQuantity = 0;
    }

    public Merchandise(String productId, String name, ProductCategory category,
                       double basePrice, int stockQuantity) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
        this.stockQuantity = stockQuantity;
        this.active = true;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = Math.max(0, stockQuantity);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Calculates the total price including applicable tax.
     *
     * @return price with tax
     */
    public double getPriceWithTax() {
        return category != null ? category.priceWithTax(basePrice) : basePrice;
    }

    /**
     * Checks whether the item is in stock.
     *
     * @return true if stock > 0
     */
    public boolean isInStock() {
        return stockQuantity > 0 && active;
    }

    /**
     * Reserves stock for a purchase.
     *
     * @param quantity the quantity to reserve
     * @return true if reservation succeeded
     */
    public boolean reserveStock(int quantity) {
        if (quantity <= 0 || quantity > stockQuantity) {
            return false;
        }
        this.stockQuantity -= quantity;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Merchandise that = (Merchandise) o;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return String.format(
            "Merchandise{id='%s', name='%s', category=%s, price=%.2f, stock=%d, size=%s, color=%s}",
            productId, name, category, basePrice, stockQuantity, size, color
        );
    }

    @Override
    public String getEntityId() {
        return productId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
            safe(productId),
            safe(name),
            safe(description),
            category != null ? category.name() : "",
            String.valueOf(basePrice),
            safe(size),
            safe(color),
            String.valueOf(stockQuantity),
            String.valueOf(active),
            safe(imageUrl)
        );
    }

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    public static Merchandise fromCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        if (parts.length < 1) return null;
        Merchandise m = new Merchandise();
        m.setProductId(getField(parts, 0, null));
        m.setName(getField(parts, 1, null));
        m.setDescription(getField(parts, 2, null));
        try { m.setCategory(ProductCategory.valueOf(getField(parts, 3, "JERSEY"))); } catch (Exception e) { /* default */ }
        m.setBasePrice(getDoubleField(parts, 4, 0.0));
        m.setSize(getField(parts, 5, null));
        m.setColor(getField(parts, 6, null));
        m.setStockQuantity(getIntField(parts, 7, 0));
        m.setActive(getBooleanField(parts, 8, true));
        m.setImageUrl(getField(parts, 9, null));
        return m;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static Merchandise fromCsv(String csv) {
        return fromCsvLine(csv);
    }

    // parseCsvLine() and safe() are inherited from BaseEntity
}
