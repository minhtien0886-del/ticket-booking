package com.club.exception;

/**
 * Thrown when a file-level locking operation fails during concurrency control.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class FileLockException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final String lockType;

    public FileLockException(String fileName, String lockType, String reason) {
        super("ERR_FILE_LOCK",
            String.format("File lock failure on '%s' (type=%s): %s", fileName, lockType, reason));
        this.fileName = fileName;
        this.lockType = lockType;
    }

    public FileLockException(String message) {
        super("ERR_FILE_LOCK", message);
        this.fileName = "UNKNOWN";
        this.lockType = "UNKNOWN";
    }

    public String getFileName() {
        return fileName;
    }

    public String getLockType() {
        return lockType;
    }
}
