package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link Transaction} entities backed by transactions.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class TransactionRepository extends GenericCsvRepository<Transaction> {

    private static final String HEADER = "transactionId,timestamp,type,amount,description,referenceId,category,processedBy,reconciled";

    public TransactionRepository(Path dataDir) {
        super(
            dataDir.resolve("transactions.csv"),
            "transactionId",
            Transaction::getTransactionId,
            Transaction::fromCsv,
            Transaction::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<Transaction> findByType(TransactionType type) {
        return findAll(t -> t.getType() == type);
    }

    public List<Transaction> findByReferenceId(String refId) {
        return findAll(t -> refId.equals(t.getReferenceId()));
    }

    public List<Transaction> findByCategory(String category) {
        return findAll(t -> category.equalsIgnoreCase(t.getCategory()));
    }

    public List<Transaction> findUnreconciled() {
        return findAll(t -> !t.isReconciled());
    }

    public double getTotalIncome() {
        return findAll().stream()
            .filter(Transaction::isIncome)
            .mapToDouble(Transaction::getAmount)
            .sum();
    }

    public double getTotalExpense() {
        return findAll().stream()
            .filter(Transaction::isExpense)
            .mapToDouble(Transaction::getAmount)
            .sum();
    }

    public double getNetBalance() {
        return getTotalIncome() - getTotalExpense();
    }

    @Override
    protected Transaction parse(String csvLine) {
        return Transaction.fromCsv(csvLine);
    }

    @Override
    protected String serialize(Transaction entity) {
        return entity.toCsv();
    }
}
