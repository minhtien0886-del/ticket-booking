package com.club.model;

import java.util.Objects;

/**
 * Represents a user account in the FCM-ERP authentication system.
 * Each account is associated with exactly one {@link Role} that determines
 * the user's permissions within the system. Accounts can be locked, suspended,
 * or disabled by administrators.
 *
 * <p>Username uniqueness is enforced at the repository layer to prevent
 * identity collisions during authentication.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 * @see Role
 * @see AccountStatus
 */
public class UserAccount extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Unique username — serves as the primary key for authentication. */
    private String username;

    /** SHA-256 hash of the user's password (never stored in plain text). */
    private String passwordHash;

    /** The role assigned to this account. */
    private Role role;

    /** Current operational status of the account. */
    private AccountStatus status;

    /** Timestamp of the last successful login (ISO format). */
    private String lastLogin;

    /** Number of consecutive failed login attempts (resets on success). */
    private int failedLoginAttempts;

    /** Timestamp of account creation (ISO format). */
    private String createdAt;

    /** The person's ID this account is linked to (player/staff/fan ID). */
    private String personId;

    /**
     * Default constructor.
     */
    public UserAccount() {
        this.status = AccountStatus.PENDING;
        this.failedLoginAttempts = 0;
    }

    /**
     * Full constructor.
     */
    public UserAccount(String username, String passwordHash, Role role,
                       AccountStatus status, String personId) {
        setUsername(username);
        this.passwordHash = passwordHash;
        this.role = role != null ? role : Role.FAN;
        this.status = status != null ? status : AccountStatus.ACTIVE;
        this.personId = personId;
        this.failedLoginAttempts = 0;
    }

    /**
     * Simplified constructor for new account creation.
     */
    public UserAccount(String username, String passwordHash, Role role, String personId) {
        this(username, passwordHash, role, AccountStatus.ACTIVE, personId);
    }

    // ============ GETTERS AND SETTERS ============

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (username.length() < 3 || username.length() > 32) {
            throw new IllegalArgumentException("Username must be between 3 and 32 characters");
        }
        if (!username.matches("^[A-Za-z0-9_]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores");
        }
        this.username = username.trim().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be null or empty");
        }
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role != null ? role : Role.FAN;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status != null ? status : AccountStatus.ACTIVE;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = Math.max(0, failedLoginAttempts);
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    // ============ BUSINESS METHODS ============

    /**
     * Checks whether this account is permitted to log in.
     *
     * @return true if login is allowed based on current status
     */
    public boolean canLogin() {
        return status.isLoginAllowed() && failedLoginAttempts < 5;
    }

    /**
     * Records a failed login attempt and auto-locks the account after 5 failures.
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.status = AccountStatus.LOCKED;
        }
    }

    /**
     * Records a successful login, resetting the failed attempt counter.
     */
    public void recordSuccessfulLogin(String timestamp) {
        this.failedLoginAttempts = 0;
        this.lastLogin = timestamp;
        if (this.status == AccountStatus.LOCKED) {
            this.status = AccountStatus.ACTIVE;
        }
    }

    /**
     * Checks whether the account has a specific permission.
     *
     * @param permission the permission to check
     * @return true if the role has the permission
     */
    public boolean hasPermission(Permission permission) {
        return role != null && role.hasPermission(permission);
    }

    /**
     * Checks whether the account is a staff account (non-fan).
     *
     * @return true if this is a staff account
     */
    public boolean isStaff() {
        return role != null && role.isStaffRole();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAccount that = (UserAccount) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return String.format(
            "UserAccount{username='%s', role=%s, status=%s, failedAttempts=%d, personId='%s'}",
            username, role, status, failedLoginAttempts, personId
        );
    }

    @Override
    public String getEntityId() {
        return username;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
            safe(username),
            safe(passwordHash),
            role != null ? role.name() : "FAN",
            status != null ? status.name() : "ACTIVE",
            safe(lastLogin),
            String.valueOf(failedLoginAttempts),
            safe(createdAt),
            safe(personId)
        );
    }

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    public static UserAccount fromCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        if (parts.length < 1) return null;
        UserAccount ua = new UserAccount();
        ua.setUsername(parts[0]);
        ua.setPasswordHash(getField(parts, 1, ""));
        try { ua.setRole(Role.valueOf(getField(parts, 2, "FAN"))); } catch (Exception e) { /* default */ }
        try { ua.setStatus(AccountStatus.valueOf(getField(parts, 3, "ACTIVE"))); } catch (Exception e) { /* default */ }
        ua.setLastLogin(getField(parts, 4, null));
        ua.setFailedLoginAttempts(getIntField(parts, 5, 0));
        ua.setCreatedAt(getField(parts, 6, null));
        ua.setPersonId(getField(parts, 7, null));
        return ua;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static UserAccount fromCsv(String csv) {
        return fromCsvLine(csv);
    }

    // parseCsvLine() and safe() are inherited from BaseEntity
}
