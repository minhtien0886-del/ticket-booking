package com.club.exception;

/**
 * Thrown when a financial report generation fails.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class ReportGenerationException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String reportType;

    public ReportGenerationException(String reportType, String reason) {
        super("ERR_REPORT_GENERATION",
            String.format("Failed to generate '%s' report: %s", reportType, reason));
        this.reportType = reportType;
    }

    public ReportGenerationException(String message) {
        super("ERR_REPORT_GENERATION", message);
        this.reportType = "UNKNOWN";
    }

    public String getReportType() {
        return reportType;
    }
}
