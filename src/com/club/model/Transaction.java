package com.club.model;

import java.util.Objects;

/**
 * Represents a financial transaction in the club's ledger.
 * Tracks all monetary movements including ticket sales, merchandise revenue,
 * salary payments, and operational expenses.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class Transaction extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String transactionId;
    private String timestamp;
    private TransactionType type;
    private double amount;
    private String description;
    private String referenceId;
    private String category;
    private String processedBy;
    private boolean reconciled;

    public Transaction() {
        this.reconciled = false;
    }

    public Transaction(String transactionId, TransactionType type, double amount, String description) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.reconciled = false;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public boolean isReconciled() {
        return reconciled;
    }

    public void setReconciled(boolean reconciled) {
        this.reconciled = reconciled;
    }

    public boolean isIncome() {
        return type != null && type.isIncome();
    }

    public boolean isExpense() {
        return type != null && type.isExpense();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return String.format(
            "Transaction{id='%s', type=%s, amount=%.2f, desc='%s', reconciled=%s}",
            transactionId, type, amount, description, reconciled
        );
    }

    @Override
    public String getEntityId() {
        return transactionId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
            safe(transactionId),
            safe(timestamp),
            type != null ? type.name() : "",
            String.valueOf(amount),
            safe(description),
            safe(referenceId),
            safe(category),
            safe(processedBy),
            String.valueOf(reconciled)
        );
    }

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    public static Transaction fromCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        if (parts.length < 1) return null;
        Transaction t = new Transaction();
        t.setTransactionId(getField(parts, 0, null));
        t.setTimestamp(getField(parts, 1, null));
        try { t.setType(TransactionType.valueOf(getField(parts, 2, "OTHER_INCOME"))); } catch (Exception e) { /* default */ }
        t.setAmount(getDoubleField(parts, 3, 0.0));
        t.setDescription(getField(parts, 4, null));
        t.setReferenceId(getField(parts, 5, null));
        t.setCategory(getField(parts, 6, null));
        t.setProcessedBy(getField(parts, 7, null));
        t.setReconciled(getBooleanField(parts, 8, false));
        return t;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static Transaction fromCsv(String csv) {
        return fromCsvLine(csv);
    }

    // parseCsvLine() and safe() are inherited from BaseEntity
}
