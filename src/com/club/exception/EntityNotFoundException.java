package com.club.exception;

/**
 * Thrown when an operation is attempted on an entity that does not exist in the system.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class EntityNotFoundException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String entityType;
    private final String entityId;

    public EntityNotFoundException(String entityType, String entityId) {
        super("ERR_ENTITY_NOT_FOUND",
            String.format("%s with ID '%s' was not found", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public EntityNotFoundException(String entityType, String field, String value) {
        super("ERR_ENTITY_NOT_FOUND",
            String.format("%s with %s '%s' was not found", entityType, field, value));
        this.entityType = entityType;
        this.entityId = value;
    }

    public EntityNotFoundException(String message) {
        super("ERR_ENTITY_NOT_FOUND", message);
        this.entityType = "UNKNOWN";
        this.entityId = "UNKNOWN";
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
