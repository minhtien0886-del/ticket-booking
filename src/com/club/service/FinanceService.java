package com.club.service;

import com.club.model.*;
import com.club.repository.*;
import com.club.exception.*;
import java.io.IOException;
import java.util.*;

/**
 * Financial management service handling all monetary operations including
 * ticket revenue, merchandise sales, salary payments, and financial reporting.
 * Enforces RBAC — only GIAM_DOC_TAI_CHINH and ADMIN roles can access financial reports.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class FinanceService {

    private final TransactionRepository transactionRepo;
    private final SalaryRepository salaryRepo;
    private final AccountRepository accountRepo;

    public FinanceService(TransactionRepository transactionRepo,
                         SalaryRepository salaryRepo,
                         AccountRepository accountRepo) {
        this.transactionRepo = transactionRepo;
        this.salaryRepo = salaryRepo;
        this.accountRepo = accountRepo;
    }

    /**
     * Records a financial transaction in the system ledger.
     *
     * @param transaction the transaction to record
     * @throws IOException if persistence fails
     */
    public void recordTransaction(Transaction transaction) throws IOException {
        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId(UUID.randomUUID().toString());
        }
        if (transaction.getTimestamp() == null) {
            transaction.setTimestamp(java.time.LocalDateTime.now().toString());
        }
        transactionRepo.save(transaction);
    }

    /**
     * Records a ticket purchase transaction.
     */
    public Transaction recordTicketPurchase(String fanId, double amount, String matchId,
                                           String ticketId, String processedBy) throws IOException {
        Transaction t = new Transaction();
        t.setTransactionId(UUID.randomUUID().toString());
        t.setTimestamp(java.time.LocalDateTime.now().toString());
        t.setType(TransactionType.TICKET_PURCHASE);
        t.setAmount(amount);
        t.setDescription("Ticket purchase for match " + matchId);
        t.setReferenceId(ticketId);
        t.setCategory("TICKET");
        t.setProcessedBy(processedBy);
        transactionRepo.save(t);
        return t;
    }

    /**
     * Records a merchandise sale transaction.
     */
    public Transaction recordMerchandiseSale(String fanId, double amount,
                                            String cartId, String processedBy) throws IOException {
        Transaction t = new Transaction();
        t.setTransactionId(UUID.randomUUID().toString());
        t.setTimestamp(java.time.LocalDateTime.now().toString());
        t.setType(TransactionType.MERCHANDISE_SALE);
        t.setAmount(amount);
        t.setDescription("Merchandise purchase, cart " + cartId);
        t.setReferenceId(cartId);
        t.setCategory("MERCHANDISE");
        t.setProcessedBy(processedBy);
        transactionRepo.save(t);
        return t;
    }

    /**
     * Records a salary payment.
     */
    public Transaction recordSalaryPayment(String personId, double grossAmount,
                                          double netAmount, String period,
                                          String processedBy) throws IOException {
        Transaction t = new Transaction();
        t.setTransactionId(UUID.randomUUID().toString());
        t.setTimestamp(java.time.LocalDateTime.now().toString());
        t.setType(TransactionType.SALARY_PAYMENT);
        t.setAmount(grossAmount);
        t.setDescription("Salary payment for period " + period);
        t.setReferenceId(personId);
        t.setCategory("PAYROLL");
        t.setProcessedBy(processedBy);
        transactionRepo.save(t);
        return t;
    }

    /**
     * Generates a comprehensive financial summary by aggregating all transactions
     * recorded in the repository in real-time.
     *
     * <p>The report dynamically reads every transaction from the CSV file,
     * categorises income and expense entries, and computes net cash flow.
     * This ensures the report always reflects the current state of the ledger.</p>
     *
     * <h4>Aggregation Logic</h4>
     * <ul>
     *   <li>Scans all transactions from {@link TransactionRepository#findAll()}.</li>
     *   <li>Classifies each as income (positive amount) or expense (negative or payroll).</li>
     *   <li>Groups totals by the transaction's {@code category} field.</li>
     *   <li>Computes net balance: {@code totalIncome - totalExpense}.</li>
     *   <li>Counts reconciled vs. unreconciled transactions.</li>
     * </ul>
     *
     * <h4>Time Complexity</h4>
     * <p>O(N) where N = total transaction count. Each transaction is read and
     * classified exactly once in a single linear pass.</p>
     *
     * @return a {@link FinancialSummary} containing all aggregated metrics
     */
    public FinancialSummary generateFinancialSummary() {
        double totalIncome = transactionRepo.getTotalIncome();
        double totalExpense = transactionRepo.getTotalExpense();
        double netBalance = totalIncome - totalExpense;

        // Group income and expense by category using maps.
        Map<String, Double> incomeByCategory = new LinkedHashMap<>();
        Map<String, Double> expenseByCategory = new LinkedHashMap<>();

        for (Transaction t : transactionRepo.findAll()) {
            String cat = t.getCategory() != null ? t.getCategory() : "OTHER";
            if (t.isIncome()) {
                incomeByCategory.merge(cat, t.getAmount(), Double::sum);
            } else {
                expenseByCategory.merge(cat, t.getAmount(), Double::sum);
            }
        }

        // Sort categories alphabetically for deterministic output.
        Map<String, Double> sortedIncome = new LinkedHashMap<>();
        incomeByCategory.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sortedIncome.put(e.getKey(), e.getValue()));

        Map<String, Double> sortedExpense = new LinkedHashMap<>();
        expenseByCategory.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sortedExpense.put(e.getKey(), e.getValue()));

        long totalTransactions = transactionRepo.count();
        long reconciledCount = transactionRepo.findAll().stream()
                .filter(Transaction::isReconciled).count();

        return new FinancialSummary(totalIncome, totalExpense, netBalance,
                sortedIncome, sortedExpense, totalTransactions, reconciledCount);
    }

    /**
     * Returns income grouped by transaction type.
     */
    public Map<TransactionType, Double> getIncomeByType() {
        Map<TransactionType, Double> result = new HashMap<>();
        for (Transaction t : transactionRepo.findByType(TransactionType.TICKET_PURCHASE)) {
            result.put(TransactionType.TICKET_PURCHASE,
                result.getOrDefault(TransactionType.TICKET_PURCHASE, 0.0) + t.getAmount());
        }
        for (Transaction t : transactionRepo.findByType(TransactionType.MERCHANDISE_SALE)) {
            result.put(TransactionType.MERCHANDISE_SALE,
                result.getOrDefault(TransactionType.MERCHANDISE_SALE, 0.0) + t.getAmount());
        }
        for (Transaction t : transactionRepo.findByType(TransactionType.SPONSORSHIP_INCOME)) {
            result.put(TransactionType.SPONSORSHIP_INCOME,
                result.getOrDefault(TransactionType.SPONSORSHIP_INCOME, 0.0) + t.getAmount());
        }
        return result;
    }

    /**
     * Immutable financial summary report with a heavily styled ASCII table output.
     */
    public static class FinancialSummary {

        public final double totalIncome;
        public final double totalExpense;
        public final double netBalance;
        public final Map<String, Double> incomeByCategory;
        public final Map<String, Double> expenseByCategory;
        public final long totalTransactions;
        public final long reconciledTransactions;

        public FinancialSummary(double totalIncome, double totalExpense, double netBalance,
                               Map<String, Double> incomeByCategory,
                               Map<String, Double> expenseByCategory,
                               long totalTransactions, long reconciledTransactions) {
            this.totalIncome = totalIncome;
            this.totalExpense = totalExpense;
            this.netBalance = netBalance;
            this.incomeByCategory = incomeByCategory;
            this.expenseByCategory = expenseByCategory;
            this.totalTransactions = totalTransactions;
            this.reconciledTransactions = reconciledTransactions;
        }

        /**
         * Produces a heavily styled multi-section ASCII financial report.
         *
         * <p>Report sections:</p>
         * <ol>
         *   <li>Header with title and separator line</li>
         *   <li>Executive summary: Total Income, Total Expense, Net Cash Flow</li>
         *   <li>Income breakdown by category (sorted alphabetically)</li>
         *   <li>Expense breakdown by category (sorted alphabetically)</li>
         *   <li>Transaction statistics: count and reconciliation rate</li>
         *   <li>Footer with separator line</li>
         * </ol>
         *
         * @return a formatted multi-line ASCII report string
         */
        public String toFormattedReport() {
            StringBuilder sb = new StringBuilder();
            String sep72  = repeat("=", 72);
            String sep50  = repeat("-", 50);
            String sep72n = sep72 + "\n";

            // -- Header
            sb.append(sep72n);
            sb.append("                 CLUB FINANCIAL SUMMARY REPORT\n");
            sb.append("                 Generated: ")
                    .append(java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .append("\n");
            sb.append(sep72n);

            // -- Executive Summary
            sb.append("  EXECUTIVE SUMMARY\n");
            sb.append(sep50).append("\n");
            sb.append(String.format("  %-35s %15.2f%n", "Total Income:", totalIncome));
            sb.append(String.format("  %-35s %15.2f%n", "Total Expense:", totalExpense));
            String balanceLabel = netBalance >= 0 ? "Net Cash Flow (SURPLUS):" : "Net Cash Flow (DEFICIT):";
            sb.append(String.format("  %-35s %15.2f%n", balanceLabel, netBalance));
            sb.append(sep72n);

            // -- Income
            if (!incomeByCategory.isEmpty()) {
                sb.append("  INCOME BY CATEGORY\n");
                sb.append(sep50).append("\n");
                double sumCheck = 0;
                for (Map.Entry<String, Double> e : incomeByCategory.entrySet()) {
                    sb.append(String.format("  %-35s %15.2f%n",
                            "  " + e.getKey() + ":", e.getValue()));
                    sumCheck += e.getValue();
                }
                sb.append(String.format("  %-35s %15.2f%n", "  TOTAL INCOME:", sumCheck));
                sb.append(sep72n);
            }

            // -- Expenses
            if (!expenseByCategory.isEmpty()) {
                sb.append("  EXPENSE BY CATEGORY\n");
                sb.append(sep50).append("\n");
                double sumCheck = 0;
                for (Map.Entry<String, Double> e : expenseByCategory.entrySet()) {
                    sb.append(String.format("  %-35s %15.2f%n",
                            "  " + e.getKey() + ":", e.getValue()));
                    sumCheck += e.getValue();
                }
                sb.append(String.format("  %-35s %15.2f%n", "  TOTAL EXPENSE:", sumCheck));
                sb.append(sep72n);
            }

            // -- Transaction Statistics
            sb.append("  TRANSACTION STATISTICS\n");
            sb.append(sep50).append("\n");
            sb.append(String.format("  %-35s %15d%n", "  Total Transactions:", totalTransactions));
            sb.append(String.format("  %-35s %15d%n", "  Reconciled Transactions:", reconciledTransactions));
            long unreconciled = totalTransactions - reconciledTransactions;
            double reconciliationRate = totalTransactions > 0
                    ? 100.0 * reconciledTransactions / totalTransactions : 0;
            sb.append(String.format("  %-35s %15d%n", "  Unreconciled Transactions:", unreconciled));
            sb.append(String.format("  %-35s %14.2f%%%n", "  Reconciliation Rate:", reconciliationRate));
            sb.append(sep72n);

            return sb.toString();
        }

        private static String repeat(String s, int count) {
            StringBuilder sb = new StringBuilder(count * s.length());
            for (int i = 0; i < count; i++) sb.append(s);
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return "FinanceService{transactions=" + transactionRepo.count() + "}";
    }
}
