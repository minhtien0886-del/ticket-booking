package com.club.controller;

import com.club.model.*;
import com.club.service.*;
import com.club.security.*;
import java.util.*;

/**
 * Controller for financial reporting operations.
 * Acts as the exclusive intermediary between the VIEW layer and FinanceService.
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class FinanceController {

    private final FinanceService financeService;
    private final SecurityContext securityContext;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
        this.securityContext = SecurityContext.getInstance();
    }

    /**
     * Generates and returns the complete financial summary report.
     *
     * <p>Required: VIEW_FINANCIAL_REPORTS permission.</p>
     *
     * @return the financial summary
     */
    public FinanceService.FinancialSummary generateFinancialSummary() {
        securityContext.requirePermission(Permission.VIEW_FINANCIAL_REPORTS);
        return financeService.generateFinancialSummary();
    }

    /**
     * Returns income totals grouped by transaction type.
     *
     * <p>Required: VIEW_FINANCIAL_REPORTS permission.</p>
     */
    public Map<TransactionType, Double> getIncomeByType() {
        securityContext.requirePermission(Permission.VIEW_FINANCIAL_REPORTS);
        return financeService.getIncomeByType();
    }
}
