package com.club.exception;

/**
 * Thrown when a repository operation fails.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class RepositoryException extends BaseClubException {

    private static final long serialVersionUID = 1L;

    private final String repositoryName;
    private final String operation;

    public RepositoryException(String repositoryName, String operation, String reason) {
        super("ERR_REPOSITORY",
            String.format("Repository '%s' failed operation '%s': %s", repositoryName, operation, reason));
        this.repositoryName = repositoryName;
        this.operation = operation;
    }

    public RepositoryException(String message) {
        super("ERR_REPOSITORY", message);
        this.repositoryName = "UNKNOWN";
        this.operation = "UNKNOWN";
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getOperation() {
        return operation;
    }
}
