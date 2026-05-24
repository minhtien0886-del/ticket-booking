package com.club.exception;

/**
 * Thrown when a refund operation fails or is requested improperly.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class RefundException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String transactionId;
    private final double refundAmount;

    public RefundException(String transactionId, double refundAmount, String reason) {
        super("ERR_REFUND",
            String.format("Refund failed for transaction '%s' (amount=%.2f): %s",
                transactionId, refundAmount, reason));
        this.transactionId = transactionId;
        this.refundAmount = refundAmount;
    }

    public RefundException(String message) {
        super("ERR_REFUND", message);
        this.transactionId = "UNKNOWN";
        this.refundAmount = 0.0;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public double getRefundAmount() {
        return refundAmount;
    }
}
