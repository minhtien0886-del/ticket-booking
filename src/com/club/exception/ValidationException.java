package com.club.exception;

/**
 * Thrown when a data validation constraint is violated.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class ValidationException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String fieldName;
    private final String fieldValue;

    public ValidationException(String fieldName, String fieldValue, String reason) {
        super("ERR_VALIDATION",
            String.format("Validation failed for field '%s' (value='%s'): %s", fieldName, fieldValue, reason));
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public ValidationException(String message) {
        super("ERR_VALIDATION", message);
        this.fieldName = "UNKNOWN";
        this.fieldValue = "UNKNOWN";
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }
}
