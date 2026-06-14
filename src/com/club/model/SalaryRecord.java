package com.club.model;

/**
 * Represents a salary payment record for employees.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class SalaryRecord extends BaseEntity {

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

    @Override
    public String getEntityId() {
        return recordId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
            safe(recordId), safe(personId), safe(personType),
            String.valueOf(grossSalary), String.valueOf(deductions),
            String.valueOf(netSalary), safe(payPeriod), safe(paymentDate),
            safe(processedBy), safe(status)
        );
    }

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    public static SalaryRecord fromCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        if (parts.length < 1) return null;
        SalaryRecord sr = new SalaryRecord();
        sr.setRecordId(getField(parts, 0, null));
        sr.setPersonId(getField(parts, 1, null));
        sr.setPersonType(getField(parts, 2, null));
        sr.setGrossSalary(getDoubleField(parts, 3, 0.0));
        sr.setDeductions(getDoubleField(parts, 4, 0.0));
        sr.setNetSalary(getDoubleField(parts, 5, 0.0));
        sr.setPayPeriod(getField(parts, 6, null));
        sr.setPaymentDate(getField(parts, 7, null));
        sr.setProcessedBy(getField(parts, 8, null));
        sr.setStatus(getField(parts, 9, null));
        return sr;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static SalaryRecord fromCsv(String csv) {
        return fromCsvLine(csv);
    }

    @Override
    public String toString() {
        return String.format("SalaryRecord{person='%s', gross=%.2f, net=%.2f, period=%s}",
            personId, grossSalary, netSalary, payPeriod);
    }
}
