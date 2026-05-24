package com.club.model;

/**
 * Enumeration of all possible account statuses within the FCM-ERP system.
 * Models the lifecycle states of a user account from creation through
 * potential suspension or permanent termination.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public enum AccountStatus {

    /** Account is active and fully operational. */
    ACTIVE("Active account — full access granted"),

    /** Account is temporarily locked due to failed login attempts or manual suspension. */
    LOCKED("Locked account — access denied, requires administrator unlock"),

    /** Account is pending email verification or administrative approval. */
    PENDING("Pending account — awaiting verification or approval"),

    /** Account has been permanently disabled and cannot be reactivated. */
    DISABLED("Disabled account — permanently deactivated"),

    /** Account is temporarily suspended for maintenance or disciplinary reasons. */
    SUSPENDED("Suspended account — temporarily barred from access");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    /**
     * Returns a description of what this status means.
     *
     * @return human-readable status description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks whether an account with this status is allowed to log in.
     *
     * @return true if login is permitted, false otherwise
     */
    public boolean isLoginAllowed() {
        return this == ACTIVE;
    }

    /**
     * Checks whether an account with this status can perform transactions.
     *
     * @return true if transactions are allowed, false otherwise
     */
    public boolean isTransactionAllowed() {
        return this == ACTIVE;
    }
}
