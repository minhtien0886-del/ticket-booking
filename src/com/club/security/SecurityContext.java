package com.club.security;

import com.club.exception.*;
import com.club.model.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe security context that holds the active {@link UserSession} for the current thread.
 *
 * <h3>ThreadLocal Session Isolation</h3>
 *
 * <p>The current session is stored in a {@code ThreadLocal<UserSession>} field,
 * guaranteeing complete isolation between concurrent threads. Each thread — whether a
 * CLI input handler, a simulator worker thread, or a web request thread — sees only
 * its own session. This prevents session-context bleeding between concurrent operations.</p>
 *
 * <p><b>Key guarantee:</b> Two threads can hold different sessions simultaneously
 * without interference. Thread A calling {@link #setCurrentSession(s)} does NOT
 * affect Thread B's current session, and vice versa. This is the foundation of
 * concurrent multi-user support in the ERP system.</p>
 *
 * <h3>Global Session Registry</h3>
 *
 * <p>A static {@code ConcurrentHashMap<String, UserSession>} tracks all active
 * sessions system-wide for administrative operations (viewing all sessions, forcing
 * logout, session count limits). This registry is shared but thread-safe via
 * ConcurrentHashMap's lock-free reads.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 * @see UserSession
 * @see Permission
 * @see Role
 */
public final class SecurityContext {

    private static final long serialVersionUID = 1L;

    /** Thread-local storage for the current session per thread. */
    private static final ThreadLocal<UserSession> currentSession = new ThreadLocal<>();

    /** Global session registry for tracking all active sessions. */
    private static final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();

    /** Maximum concurrent sessions allowed per user account. */
    private static final int MAX_SESSIONS_PER_USER = 3;

    /** Singleton instance for static access. */
    private static volatile SecurityContext instance;

    /**
     * Private constructor for singleton pattern.
     */
    private SecurityContext() {}

    /**
     * Returns the singleton instance of the SecurityContext.
     *
     * @return the singleton instance
     */
    public static SecurityContext getInstance() {
        if (instance == null) {
            synchronized (SecurityContext.class) {
                if (instance == null) {
                    instance = new SecurityContext();
                }
            }
        }
        return instance;
    }

    // ============ SESSION MANAGEMENT ============

    /**
     * Establishes the current session for this thread.
     *
     * @param session the user session to set as current
     * @throws SessionException if a session is already active on this thread
     */
    public void setCurrentSession(UserSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        UserSession existing = currentSession.get();
        if (existing != null && !existing.isTerminated()) {
            throw new SessionException(existing.getSessionId(),
                "A session is already active on this thread. Terminate it first.");
        }

        int currentSessionCount = countSessionsForUser(session.getUsername());
        if (currentSessionCount >= MAX_SESSIONS_PER_USER) {
            throw new SessionException(session.getSessionId(),
                "Maximum concurrent sessions (" + MAX_SESSIONS_PER_USER + ") exceeded for user: " + session.getUsername());
        }

        currentSession.set(session);
        activeSessions.put(session.getSessionId(), session);
    }

    /**
     * Retrieves the current session for this thread.
     *
     * @return the current UserSession, or null if no session is active
     */
    public UserSession getCurrentSession() {
        return currentSession.get();
    }

    /**
     * Clears the current session from this thread without global removal.
     * Use this for temporary session suspension.
     */
    public void clearCurrentSession() {
        currentSession.remove();
    }

    /**
     * Terminates the current session both locally and globally.
     *
     * @param reason the reason for termination
     * @return the terminated session
     * @throws SessionException if no session is active
     */
    public UserSession terminateCurrentSession(String reason) {
        UserSession session = currentSession.get();
        if (session == null) {
            throw new SessionException("No active session to terminate");
        }
        session.terminate(reason);
        activeSessions.remove(session.getSessionId());
        currentSession.remove();
        return session;
    }

    /**
     * Retrieves a session by its ID from the global registry.
     *
     * @param sessionId the session ID to look up
     * @return the UserSession if found
     * @throws SessionException if the session is not found
     */
    public UserSession getSession(String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new SessionException(sessionId, "Session not found or already terminated");
        }
        return session;
    }

    // ============ PERMISSION ENFORCEMENT ============

    /**
     * Checks whether the current session has the specified permission.
     * Returns false without throwing an exception if the permission is missing.
     *
     * @param permission the permission to check
     * @return true if the current session has the permission
     * @throws SessionException if no session is active
     */
    public boolean checkPermission(Permission permission) {
        UserSession session = requireSession();
        return session.hasPermission(permission);
    }

    /**
     * Checks whether the current session has ALL specified permissions.
     *
     * @param permissions the permissions to check
     * @return true if all permissions are present
     * @throws SessionException if no session is active
     */
    public boolean checkAllPermissions(Permission... permissions) {
        UserSession session = requireSession();
        return session.hasAllPermissions(permissions);
    }

    /**
     * Requires the specified permission, throwing AccessDeniedException if missing.
     * This is the primary method for enforcing authorization in service layers.
     *
     * @param permission the required permission
     * @throws AccessDeniedException if the permission is not granted
     * @throws SessionException if no session is active
     */
    public void requirePermission(Permission permission) {
        UserSession session = requireSession();
        if (!session.hasPermission(permission)) {
            throw new AccessDeniedException(
                session.getUsername(),
                permission.name(),
                "execute operation"
            );
        }
    }

    /**
     * Requires ALL specified permissions, throwing AccessDeniedException if any are missing.
     *
     * @param permissions the required permissions
     * @throws AccessDeniedException if any permission is not granted
     * @throws SessionException if no session is active
     */
    public void requireAllPermissions(Permission... permissions) {
        UserSession session = requireSession();
        for (Permission p : permissions) {
            if (!session.hasPermission(p)) {
                throw new AccessDeniedException(
                    session.getUsername(),
                    p.name(),
                    "execute operation"
                );
            }
        }
    }

    /**
     * Requires the specified role, throwing AccessDeniedException if the role does not match.
     *
     * @param role the required role
     * @throws AccessDeniedException if the role does not match
     * @throws SessionException if no session is active
     */
    public void requireRole(Role role) {
        UserSession session = requireSession();
        if (session.getRole() != role) {
            throw new AccessDeniedException(
                session.getUsername(),
                "Role " + role.name() + " required",
                "execute operation"
            );
        }
    }

    /**
     * Requires that the current session is a staff account (non-FAN).
     *
     * @throws AccessDeniedException if the session belongs to a FAN
     * @throws SessionException if no session is active
     */
    public void requireStaff() {
        UserSession session = requireSession();
        if (!session.isStaff()) {
            throw new AccessDeniedException(
                session.getUsername(),
                "STAFF role required",
                "This operation is restricted to staff members only"
            );
        }
    }

    /**
     * Requires that the current session is an admin account.
     *
     * @throws AccessDeniedException if the session does not belong to an admin
     * @throws SessionException if no session is active
     */
    public void requireAdmin() {
        UserSession session = requireSession();
        if (!session.isAdmin()) {
            throw new AccessDeniedException(
                session.getUsername(),
                "ADMIN role required",
                "This operation requires administrator privileges"
            );
        }
    }

    /**
     * Validates that the current session can perform transactions.
     *
     * @throws SessionException if no session is active or transactions are not allowed
     */
    public void requireActiveTransaction() {
        UserSession session = requireSession();
        session.validate();
    }

    // ============ HELPER METHODS ============

    private UserSession requireSession() throws SessionException {
        UserSession session = currentSession.get();
        if (session == null) {
            throw new SessionException("No active session. Authentication required.");
        }
        if (session.isTerminated()) {
            throw new SessionException(session.getSessionId(), "Session has been terminated");
        }
        session.recordActivity();
        return session;
    }

    private int countSessionsForUser(String username) {
        return (int) activeSessions.values().stream()
            .filter(s -> s.getUsername().equals(username) && !s.isTerminated())
            .count();
    }

    /**
     * Returns the total number of active sessions.
     *
     * @return count of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Returns the count of active sessions for a specific user.
     *
     * @param username the username to query
     * @return number of active sessions
     */
    public int getActiveSessionCountForUser(String username) {
        return countSessionsForUser(username);
    }

    /**
     * Returns an unmodifiable view of all active sessions.
     *
     * @return map of session ID to session
     */
    public Map<String, UserSession> getActiveSessions() {
        return java.util.Collections.unmodifiableMap(activeSessions);
    }

    /**
     * Terminates all sessions for a specific user.
     *
     * @param username the username whose sessions to terminate
     * @return the number of sessions terminated
     */
    public int terminateAllSessionsForUser(String username) {
        int count = 0;
        for (Map.Entry<String, UserSession> entry : activeSessions.entrySet()) {
            if (entry.getValue().getUsername().equals(username) && !entry.getValue().isTerminated()) {
                entry.getValue().terminate("Admin forced logout");
                activeSessions.remove(entry.getKey());
                count++;
            }
        }
        return count;
    }

    /**
     * Clears the entire session registry. Use with caution.
     */
    public void clearAllSessions() {
        activeSessions.clear();
        currentSession.remove();
    }

    @Override
    public String toString() {
        return String.format("SecurityContext{activeSessions=%d}", activeSessions.size());
    }
}
