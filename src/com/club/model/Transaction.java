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
public class Transaction {

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

    public String toCsv() {
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

    private String safe(String s) {
        if (s == null) return "";
        return s.contains(",") ? "\"" + s + "\"" : s;
    }

    public static Transaction fromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        Transaction t = new Transaction();
        t.setTransactionId(parts.length > 0 ? parts[0] : null);
        t.setTimestamp(parts.length > 1 ? parts[1] : null);
        try { t.setType(TransactionType.valueOf(parts.length > 2 ? parts[2] : "OTHER_INCOME")); } catch (Exception e) { /* default */ }
        try { t.setAmount(Double.parseDouble(parts.length > 3 ? parts[3] : "0")); } catch (Exception e) { /* default 0 */ }
        t.setDescription(parts.length > 4 ? parts[4] : null);
        t.setReferenceId(parts.length > 5 ? parts[5] : null);
        t.setCategory(parts.length > 6 ? parts[6] : null);
        t.setProcessedBy(parts.length > 7 ? parts[7] : null);
        try { t.setReconciled(Boolean.parseBoolean(parts.length > 8 ? parts[8] : "false")); } catch (Exception e) { /* default false */ }
        return t;
    }

    private static String[] parseCsvLine(String csv) {
        java.util.List<String> result = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : csv.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
