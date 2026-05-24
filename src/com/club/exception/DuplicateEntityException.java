package com.club.exception;

/**
 * Thrown when attempting to create an entity that violates a unique constraint,
 * such as a duplicate username or seat ID.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class DuplicateEntityException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String entityType;
    private final String fieldName;
    private final String duplicateValue;

    public DuplicateEntityException(String entityType, String fieldName, String duplicateValue) {
        super("ERR_DUPLICATE_ENTITY",
            String.format("Duplicate %s: field '%s' with value '%s' already exists",
                entityType, fieldName, duplicateValue));
        this.entityType = entityType;
        this.fieldName = fieldName;
        this.duplicateValue = duplicateValue;
    }

    public DuplicateEntityException(String message) {
        super("ERR_DUPLICATE_ENTITY", message);
        this.entityType = "UNKNOWN";
        this.fieldName = "UNKNOWN";
        this.duplicateValue = "UNKNOWN";
    }

    public String getEntityType() {
        return entityType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getDuplicateValue() {
        return duplicateValue;
    }
}
