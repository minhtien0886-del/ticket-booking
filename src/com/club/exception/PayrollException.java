package com.club.exception;

/**
 * Thrown when a payroll operation fails or salary data is invalid.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class PayrollException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String personId;
    private final double salaryAmount;

    public PayrollException(String personId, double salaryAmount, String reason) {
        super("ERR_PAYROLL",
            String.format("Payroll error for person '%s' (amount=%.2f): %s", personId, salaryAmount, reason));
        this.personId = personId;
        this.salaryAmount = salaryAmount;
    }

    public PayrollException(String message) {
        super("ERR_PAYROLL", message);
        this.personId = "UNKNOWN";
        this.salaryAmount = 0.0;
    }

    public String getPersonId() {
        return personId;
    }

    public double getSalaryAmount() {
        return salaryAmount;
    }
}
