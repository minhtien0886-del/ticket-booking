package com.club.model;

/**
 * Enumeration of all financial transaction types in the club's ledger.
 * Used for accurate bookkeeping, audit trails, and financial reporting.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public enum TransactionType {

    TICKET_PURCHASE("Ticket purchase revenue", true),
    MERCHANDISE_SALE("Merchandise sale revenue", true),
    SEASON_TICKET_SALE("Season ticket revenue", true),
    SPONSORSHIP_INCOME("Sponsorship and partnership income", true),
    BROADCASTING_REVENUE("TV broadcasting rights revenue", true),
    PRIZE_MONEY("Tournament prize money", true),
    PLAYER_TRANSFER_IN("Income from player transfers", true),
    PLAYER_TRANSFER_OUT("Expenditure on player purchases", false),
    SALARY_PAYMENT("Staff and player salary payments", false),
    MERCHANDISE_PURCHASE("Cost of goods purchased for resale", false),
    MAINTENANCE_EXPENSE("Stadium and facility maintenance", false),
    MARKETING_EXPENSE("Marketing and advertising expenditure", false),
    UTILITY_EXPENSE("Utility bills (electricity, water, etc.)", false),
    INSURANCE_EXPENSE("Club and player insurance premiums", false),
    REFUND("Customer refund payment", false),
    LOYALTY_REDEMPTION("Points redeemed for rewards", false),
    LOAN_REPAYMENT("Loan principal and interest repayment", false),
    OTHER_INCOME("Miscellaneous income", true),
    OTHER_EXPENSE("Miscellaneous expense", false);

    private final String description;
    private final boolean isIncome;

    TransactionType(String description, boolean isIncome) {
        this.description = description;
        this.isIncome = isIncome;
    }

    public String getDescription() {
        return description;
    }

    public boolean isIncome() {
        return isIncome;
    }

    /**
     * Checks whether this transaction type represents revenue (income).
     *
     * @return true if it is an income transaction
     */
    public boolean isRevenue() {
        return isIncome;
    }

    /**
     * Checks whether this transaction type represents an expense.
     *
     * @return true if it is an expense transaction
     */
    public boolean isExpense() {
        return !isIncome;
    }
}
