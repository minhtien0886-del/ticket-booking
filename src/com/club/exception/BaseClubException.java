package com.club.exception;

/**
 * Abstract base exception for all FCM-ERP system exceptions.
 * Provides a consistent exception hierarchy and standard error reporting.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public abstract class BaseClubException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final long timestamp;

    protected BaseClubException(String message) {
        super(message);
        this.errorCode = "ERR_" + this.getClass().getSimpleName().toUpperCase();
        this.timestamp = System.currentTimeMillis();
    }

    protected BaseClubException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ERR_" + this.getClass().getSimpleName().toUpperCase();
        this.timestamp = System.currentTimeMillis();
    }

    protected BaseClubException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    protected BaseClubException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedMessage() {
        return String.format("[%s] %s (code=%s, ts=%d)", getClass().getSimpleName(), getMessage(), errorCode, timestamp);
    }
}
