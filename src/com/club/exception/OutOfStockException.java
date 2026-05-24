package com.club.exception;

/**
 * Thrown when an operation fails because a required resource is out of stock.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class OutOfStockException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String productId;
    private final String productName;
    private final int requestedQuantity;
    private final int availableQuantity;

    public OutOfStockException(String productId, String productName, int requested, int available) {
        super("ERR_OUT_OF_STOCK",
            String.format("Product '%s' (%s): requested %d, only %d available",
                productId, productName, requested, available));
        this.productId = productId;
        this.productName = productName;
        this.requestedQuantity = requested;
        this.availableQuantity = available;
    }

    public OutOfStockException(String message) {
        super("ERR_OUT_OF_STOCK", message);
        this.productId = "UNKNOWN";
        this.productName = "UNKNOWN";
        this.requestedQuantity = 0;
        this.availableQuantity = 0;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
