package com.club.exception;

/**
 * Thrown when a concurrency operation times out.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class ConcurrencyTimeoutException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final long timeoutMs;
    private final String operation;

    public ConcurrencyTimeoutException(String operation, long timeoutMs) {
        super("ERR_CONCURRENCY_TIMEOUT",
            String.format("Operation '%s' timed out after %dms", operation, timeoutMs));
        this.timeoutMs = timeoutMs;
        this.operation = operation;
    }

    public ConcurrencyTimeoutException(String message) {
        super("ERR_CONCURRENCY_TIMEOUT", message);
        this.timeoutMs = 0;
        this.operation = "UNKNOWN";
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public String getOperation() {
        return operation;
    }
}
