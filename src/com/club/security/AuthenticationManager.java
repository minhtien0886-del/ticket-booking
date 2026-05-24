package com.club.security;

import com.club.model.*;
import com.club.exception.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication manager providing secure login, logout, and credential management
 * for the FCM-ERP system. Implements password hashing using SHA-256 and manages
 * session lifecycle through the {@link SecurityContext}.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class AuthenticationManager {

    private static final long serialVersionUID = 1L;

    private static volatile AuthenticationManager instance;

    /** Secure random generator for salt generation. */
    private final SecureRandom random = new SecureRandom();

    /** The security context for session management. */
    private final SecurityContext securityContext;

    /** Repository for looking up accounts (injected). */
    private com.club.repository.AccountRepository accountRepository;

    /** Repository for looking up fans (injected). */
    private com.club.repository.FanRepository fanRepository;

    /** Repository for looking up staff (injected). */
    private com.club.repository.StaffRepository staffRepository;

    /** Repository for looking up players (injected). */
    private com.club.repository.PlayerRepository playerRepository;

    private AuthenticationManager() {
        this.securityContext = SecurityContext.getInstance();
    }

    public static AuthenticationManager getInstance() {
        if (instance == null) {
            synchronized (AuthenticationManager.class) {
                if (instance == null) {
                    instance = new AuthenticationManager();
                }
            }
        }
        return instance;
    }

    public void setAccountRepository(com.club.repository.AccountRepository repo) {
        this.accountRepository = repo;
    }

    public void setFanRepository(com.club.repository.FanRepository repo) {
        this.fanRepository = repo;
    }

    public void setStaffRepository(com.club.repository.StaffRepository repo) {
        this.staffRepository = repo;
    }

    public void setPlayerRepository(com.club.repository.PlayerRepository repo) {
        this.playerRepository = repo;
    }

    // ============ PASSWORD HASHING ============

    /**
     * Hashes a plain-text password using SHA-256 with a random salt.
     *
     * @param plainPassword the plain-text password
     * @return the hashed password string (salt:hash format)
     */
    public String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashed = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            String hashBase64 = Base64.getEncoder().encodeToString(hashed);
            return saltBase64 + ":" + hashBase64;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies a plain-text password against a stored salted hash using
     * constant-time comparison to prevent timing attacks.
     *
     * <p><b>Timing Attack Prevention:</b> The comparison uses
     * {@link MessageDigest#isEqual(byte[], byte[])} which performs a
     * constant-time byte-by-byte comparison regardless of where the first
     * difference occurs. Using {@code String.equals()} or
     * {@code byte[] ==} would leak information about the correct hash
     * through the time taken to return: an attacker who can measure
     * response latency to microsecond precision could gradually deduce
     * each byte of the stored hash and reconstruct it.</p>
     *
     * <p>The stored format is {@code saltBase64:hashBase64}.</p>
     *
     * @param plainPassword the plain-text password to verify
     * @param storedHash   the stored hash in {@code salt:hash} format
     * @return true if the password matches, false otherwise
     */
    public boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || !storedHash.contains(":")) {
            return false;
        }
        int colonIdx = storedHash.indexOf(':');
        if (colonIdx <= 0 || colonIdx >= storedHash.length() - 1) {
            return false;
        }
        String saltBase64 = storedHash.substring(0, colonIdx);
        String hashBase64 = storedHash.substring(colonIdx + 1);

        byte[] salt;
        byte[] storedHashBytes;
        try {
            salt = Base64.getDecoder().decode(saltBase64);
            storedHashBytes = Base64.getDecoder().decode(hashBase64);
        } catch (IllegalArgumentException e) {
            return false;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] computed = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            // Constant-time comparison — examines every byte regardless of match position.
            return MessageDigest.isEqual(computed, storedHashBytes);
        } catch (Exception e) {
            return false;
        }
    }

    // ============ SESSION MANAGEMENT ============

    /**
     * Authenticates a user with username and password, creating a new session.
     *
     * @param username the username
     * @param password the plain-text password
     * @param ipAddress the client's IP address
     * @return the newly created UserSession
     * @throws AuthenticationException if authentication fails
     */
    public UserSession login(String username, String password, String ipAddress) {
        if (username == null || username.trim().isEmpty()) {
            throw new AuthenticationException("Username cannot be empty");
        }
        if (password == null || password.isEmpty()) {
            throw new AuthenticationException("Password cannot be empty");
        }

        if (accountRepository == null) {
            throw new IllegalStateException("Account repository not configured");
        }

        UserAccount account = accountRepository.findByUsername(username.trim().toLowerCase());
        if (account == null) {
            throw new AuthenticationException(username, "Account not found");
        }

        if (!account.canLogin()) {
            throw new AuthenticationException(username,
                "Account cannot log in: " + account.getStatus());
        }

        if (!verifyPassword(password, account.getPasswordHash())) {
            account.recordFailedLogin();
            try { accountRepository.save(account); } catch (IOException e) { /* ignore */ }
            throw new AuthenticationException(username, "Invalid password");
        }

        Person person = resolvePerson(account.getPersonId(), account.getRole());

        account.recordSuccessfulLogin(java.time.LocalDateTime.now().toString());
        try { accountRepository.save(account); } catch (IOException e) { /* ignore */ }

        String sessionId = UUID.randomUUID().toString();
        UserSession session = new UserSession(sessionId, account, person, ipAddress);
        securityContext.setCurrentSession(session);

        return session;
    }

    /**
     * Logs out the current session.
     *
     * @param reason the reason for logout
     * @throws SessionException if no session is active
     */
    public void logout(String reason) {
        securityContext.terminateCurrentSession(reason != null ? reason : "User logout");
    }

    /**
     * Logs out the current session with a default reason.
     *
     * @throws SessionException if no session is active
     */
    public void logout() {
        logout("User logout");
    }

    /**
     * Retrieves the current active session.
     *
     * @return the current UserSession
     * @throws SessionException if no session is active
     */
    public UserSession getCurrentSession() {
        UserSession session = securityContext.getCurrentSession();
        if (session == null) {
            throw new SessionException("No active session");
        }
        return session;
    }

    /**
     * Checks whether a user is currently logged in.
     *
     * @return true if a session is active
     */
    public boolean isLoggedIn() {
        return securityContext.getCurrentSession() != null;
    }

    // ============ HELPERS ============

    private Person resolvePerson(String personId, Role role) {
        if (personId == null) return null;
        try {
            switch (role) {
                case FAN:
                    return fanRepository != null ? fanRepository.findById(personId) : null;
                case HLV_TRUONG:
                case GIAM_DOC_NHAN_SU:
                case TRONG_TAI:
                case QUAN_LY_QUAY:
                case GIAM_DOC_TAI_CHINH:
                case ADMIN:
                    return staffRepository != null ? staffRepository.findById(personId) : null;
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("AuthenticationManager{sessions=%d}",
            securityContext.getActiveSessionCount());
    }
}
