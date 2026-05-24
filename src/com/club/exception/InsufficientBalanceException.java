package com.club.exception;

/**
 * Thrown when a financial transaction fails due to insufficient account balance
 * or other payment processing issues.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class InsufficientBalanceException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String accountId;
    private final double currentBalance;
    private final double requiredAmount;

    public InsufficientBalanceException(String accountId, double currentBalance, double requiredAmount) {
        super("ERR_INSUFFICIENT_BALANCE",
            String.format("Account '%s' has insufficient balance: %.2f available, %.2f required",
                accountId, currentBalance, requiredAmount));
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }

    public String getAccountId() {
        return accountId;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public double getRequiredAmount() {
        return requiredAmount;
    }

    public double getShortfall() {
        return requiredAmount - currentBalance;
    }
}
