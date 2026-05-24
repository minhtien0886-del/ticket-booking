package com.club.exception;

/**
 * Thrown when CSV data file operations fail (read/write/parse errors).
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class CsvDataException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final int lineNumber;

    public CsvDataException(String fileName, int lineNumber, String reason) {
        super("ERR_CSV_DATA",
            String.format("CSV error in file '%s' at line %d: %s", fileName, lineNumber, reason));
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public CsvDataException(String fileName, String reason) {
        super("ERR_CSV_DATA", String.format("CSV error in file '%s': %s", fileName, reason));
        this.fileName = fileName;
        this.lineNumber = -1;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
