package com.club.model;

/**
 * Represents a salary payment record for employees.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class SalaryRecord {

    private static final long serialVersionUID = 1L;

    private String recordId;
    private String personId;
    private String personType;
    private double grossSalary;
    private double deductions;
    private double netSalary;
    private String payPeriod;
    private String paymentDate;
    private String processedBy;
    private String status;

    public SalaryRecord() {}

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getPersonType() {
        return personType;
    }

    public void setPersonType(String personType) {
        this.personType = personType;
    }

    public double getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(double grossSalary) {
        this.grossSalary = grossSalary;
    }

    public double getDeductions() {
        return deductions;
    }

    public void setDeductions(double deductions) {
        this.deductions = deductions;
    }

    public double getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(double netSalary) {
        this.netSalary = netSalary;
    }

    public String getPayPeriod() {
        return payPeriod;
    }

    public void setPayPeriod(String payPeriod) {
        this.payPeriod = payPeriod;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String toCsv() {
        return String.join(",",
            safe(recordId), safe(personId), safe(personType),
            String.valueOf(grossSalary), String.valueOf(deductions),
            String.valueOf(netSalary), safe(payPeriod), safe(paymentDate),
            safe(processedBy), safe(status)
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.contains(",") ? "\"" + s + "\"" : s;
    }

    public static SalaryRecord fromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = csv.split(",", -1);
        SalaryRecord sr = new SalaryRecord();
        sr.setRecordId(parts.length > 0 ? parts[0] : null);
        sr.setPersonId(parts.length > 1 ? parts[1] : null);
        sr.setPersonType(parts.length > 2 ? parts[2] : null);
        try { sr.setGrossSalary(Double.parseDouble(parts.length > 3 ? parts[3] : "0")); } catch (Exception e) { /* default 0 */ }
        try { sr.setDeductions(Double.parseDouble(parts.length > 4 ? parts[4] : "0")); } catch (Exception e) { /* default 0 */ }
        try { sr.setNetSalary(Double.parseDouble(parts.length > 5 ? parts[5] : "0")); } catch (Exception e) { /* default 0 */ }
        sr.setPayPeriod(parts.length > 6 ? parts[6] : null);
        sr.setPaymentDate(parts.length > 7 ? parts[7] : null);
        sr.setProcessedBy(parts.length > 8 ? parts[8] : null);
        sr.setStatus(parts.length > 9 ? parts[9] : null);
        return sr;
    }

    @Override
    public String toString() {
        return String.format("SalaryRecord{person='%s', gross=%.2f, net=%.2f, period=%s}",
            personId, grossSalary, netSalary, payPeriod);
    }
}
