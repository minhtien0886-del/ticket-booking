package com.club.exception;

/**
 * Thrown when a ticket booking operation fails due to concurrency conflicts,
 * such as double-booking attempts or version mismatches.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class OptimisticLockException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String entityId;
    private final int expectedVersion;
    private final int actualVersion;
    private final int retryCount;

    public OptimisticLockException(String entityId, int expectedVersion, int actualVersion) {
        super("ERR_OPTIMISTIC_LOCK",
            String.format("Optimistic lock conflict on entity '%s': expected version %d but found %d",
                entityId, expectedVersion, actualVersion));
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
        this.retryCount = 0;
    }

    public OptimisticLockException(String entityId, int expectedVersion, int actualVersion, int retryCount) {
        super("ERR_OPTIMISTIC_LOCK",
            String.format("Optimistic lock conflict on entity '%s': expected v%d, found v%d after %d retries",
                entityId, expectedVersion, actualVersion, retryCount));
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
        this.retryCount = retryCount;
    }

    public String getEntityId() {
        return entityId;
    }

    public int getExpectedVersion() {
        return expectedVersion;
    }

    public int getActualVersion() {
        return actualVersion;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
