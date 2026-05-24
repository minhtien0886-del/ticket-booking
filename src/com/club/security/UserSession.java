package com.club.security;

import com.club.model.*;
import com.club.exception.*;
import java.util.Objects;

/**
 * Represents an active user session within the FCM-ERP system.
 * Thread-safe session object that holds authentication state, role information,
 * and permission context for the duration of a user's login session.
 *
 * <p>Each session is identified by a unique session ID and tracks the associated
 * user account, the person's details, and session metadata including login time
 * and last activity timestamp.</p>
 *
 * <p>Session objects are stored in the thread-local {@link SecurityContext} to
 * provide secure, isolated access per concurrent thread.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 * @see SecurityContext
 * @see UserAccount
 * @see Role
 */
public final class UserSession {

    private static final long serialVersionUID = 1L;

    /** Unique session identifier (UUID). */
    private final String sessionId;

    /** The authenticated user account. */
    private final UserAccount account;

    /** The associated person entity (Player, Staff, or Fan). */
    private final Person person;

    /** Timestamp when the session was created (ISO format). */
    private final String loginTime;

    /** Timestamp of the last recorded activity (ISO format). */
    private volatile String lastActivity;

    /** IP address from which the user logged in. */
    private final String ipAddress;

    /** Hostname of the client (if resolvable). */
    private final String hostname;

    /** Whether this session has been terminated. */
    private volatile boolean terminated;

    /** Reason for session termination. */
    private volatile String terminationReason;

    /**
     * Constructs a new user session with the given account and person.
     *
     * @param sessionId  unique session identifier
     * @param account   the authenticated user account
     * @param person    the associated person entity
     * @param ipAddress the client's IP address
     */
    public UserSession(String sessionId, UserAccount account, Person person, String ipAddress) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        if (account == null) {
            throw new IllegalArgumentException("User account cannot be null");
        }
        this.sessionId = sessionId;
        this.account = account;
        this.person = person;
        this.ipAddress = ipAddress != null ? ipAddress : "127.0.0.1";
        this.hostname = "localhost";
        this.loginTime = java.time.LocalDateTime.now().toString();
        this.lastActivity = this.loginTime;
        this.terminated = false;
    }

    // ============ CORE GETTERS ============

    public String getSessionId() {
        return sessionId;
    }

    public UserAccount getAccount() {
        return account;
    }

    public Person getPerson() {
        return person;
    }

    public String getLoginTime() {
        return loginTime;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    // ============ DELEGATED ACCESSORS ============

    /**
     * Returns the username of the session's account.
     *
     * @return username string
     */
    public String getUsername() {
        return account.getUsername();
    }

    /**
     * Returns the role assigned to the session's account.
     *
     * @return the Role enum
     */
    public Role getRole() {
        return account.getRole();
    }

    /**
     * Returns the permission set for the session's role.
     *
     * @return set of permissions
     */
    public java.util.Set<Permission> getPermissions() {
        return account.getRole().getPermissions();
    }

    /**
     * Returns the person ID (fan/player/staff UUID).
     *
     * @return person ID string
     */
    public String getPersonId() {
        return person != null ? person.getId() : null;
    }

    /**
     * Returns the person type discriminator.
     *
     * @return PLAYER, STAFF, or FAN
     */
    public String getPersonType() {
        return person != null ? person.getPersonType() : "UNKNOWN";
    }

    /**
     * Returns the person's display name.
     *
     * @return name string
     */
    public String getPersonName() {
        return person != null ? person.getName() : "Unknown";
    }

    // ============ PERMISSION CHECKS ============

    /**
     * Checks whether this session has a specific permission.
     *
     * @param permission the permission to check
     * @return true if the session has the permission
     */
    public boolean hasPermission(Permission permission) {
        return account.hasPermission(permission);
    }

    /**
     * Checks whether this session has all specified permissions.
     *
     * @param permissions the permissions to check
     * @return true if all permissions are present
     */
    public boolean hasAllPermissions(Permission... permissions) {
        return account.getRole().hasAllPermissions(permissions);
    }

    /**
     * Checks whether this session belongs to a staff role.
     *
     * @return true if the session is a staff account
     */
    public boolean isStaff() {
        return account.isStaff();
    }

    /**
     * Checks whether this session belongs to the ADMIN role.
     *
     * @return true if the session is an admin
     */
    public boolean isAdmin() {
        return account.getRole() == Role.ADMIN;
    }

    /**
     * Checks whether this session belongs to the FAN role.
     *
     * @return true if the session is a fan
     */
    public boolean isFan() {
        return account.getRole() == Role.FAN;
    }

    /**
     * Checks whether the session account is allowed to perform transactions.
     *
     * @return true if transactions are permitted
     */
    public boolean canTransact() {
        return account.getStatus().isTransactionAllowed() && !terminated;
    }

    /**
     * Checks whether the session account is allowed to log in.
     *
     * @return true if login is permitted
     */
    public boolean canLogin() {
        return account.canLogin() && !terminated;
    }

    // ============ SESSION LIFECYCLE ============

    /**
     * Updates the last activity timestamp to the current time.
     */
    public void recordActivity() {
        this.lastActivity = java.time.LocalDateTime.now().toString();
    }

    /**
     * Terminates the session with a reason.
     *
     * @param reason the reason for termination
     */
    public void terminate(String reason) {
        this.terminated = true;
        this.terminationReason = reason;
        this.lastActivity = java.time.LocalDateTime.now().toString();
    }

    /**
     * Validates that the session is active and can perform operations.
     *
     * @throws SessionException if the session is terminated or invalid
     */
    public void validate() throws SessionException {
        if (this.terminated) {
            throw new SessionException(sessionId, "Session has been terminated: " + terminationReason);
        }
        if (!account.getStatus().isTransactionAllowed()) {
            throw new SessionException(sessionId, "Account status does not allow transactions: " + account.getStatus());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSession that = (UserSession) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return String.format(
            "UserSession{sessionId='%s', username='%s', role=%s, personType=%s, personName='%s', terminated=%s}",
            sessionId, getUsername(), getRole(), getPersonType(), getPersonName(), terminated
        );
    }
}
