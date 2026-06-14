package com.club.model;

import java.util.Objects;

/**
 * Represents an item within a fan's shopping cart during the browsing session.
 * Each cart item references a merchandise product with specific variant selections.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class CartItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String cartItemId;
    private String fanId;
    private String productId;
    private String productName;
    private int quantity;
    private String size;
    private String color;
    private double unitPrice;
    private String addedAt;

    public CartItem() {
        this.quantity = 1;
    }

    public CartItem(String cartItemId, String fanId, String productId, int quantity) {
        this.cartItemId = cartItemId;
        this.fanId = fanId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(String cartItemId) {
        this.cartItemId = cartItemId;
    }

    public String getFanId() {
        return fanId;
    }

    public void setFanId(String fanId) {
        this.fanId = fanId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
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

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(String addedAt) {
        this.addedAt = addedAt;
    }

    public double getSubtotal() {
        return unitPrice * quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return Objects.equals(cartItemId, cartItem.cartItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartItemId);
    }

    @Override
    public String toString() {
        return String.format(
            "CartItem{id='%s', fan='%s', product='%s', qty=%d, price=%.2f, subtotal=%.2f}",
            cartItemId, fanId, productId, quantity, unitPrice, getSubtotal()
        );
    }

    @Override
    public String getEntityId() {
        return cartItemId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
            safe(cartItemId),
            safe(fanId),
            safe(productId),
            safe(productName),
            String.valueOf(quantity),
            safe(size),
            safe(color),
            String.valueOf(unitPrice),
            safe(addedAt)
        );
    }

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    public static CartItem fromCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        if (parts.length < 1) return null;
        CartItem ci = new CartItem();
        ci.setCartItemId(getField(parts, 0, null));
        ci.setFanId(getField(parts, 1, null));
        ci.setProductId(getField(parts, 2, null));
        ci.setProductName(getField(parts, 3, null));
        ci.setQuantity(getIntField(parts, 4, 1));
        ci.setSize(getField(parts, 5, null));
        ci.setColor(getField(parts, 6, null));
        ci.setUnitPrice(getDoubleField(parts, 7, 0.0));
        ci.setAddedAt(getField(parts, 8, null));
        return ci;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static CartItem fromCsv(String csv) {
        return fromCsvLine(csv);
    }

    // parseCsvLine() and safe() are inherited from BaseEntity
}
