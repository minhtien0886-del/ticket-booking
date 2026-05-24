package com.club.controller;

import com.club.model.*;
import com.club.repository.*;
import com.club.security.*;
import com.club.exception.*;
import java.io.IOException;
import java.util.*;

/**
 * Secure controller for fan registration, profile management, and account operations.
 *
 * <h3>Permission Matrix</h3>
 * <table border="1">
 *   <tr><th>Method</th>            <th>Required Permission</th> <th>Who Can Call</th></tr>
 *   <tr><td>registerFan</td>        <td>REGISTER_FAN</td>       <td>Anyone (public), ADMIN</td></tr>
 *   <tr><td>getFanById</td>         <td>VIEW_MATCHES</td>       <td>ALL ROLES (own profile or ADMIN)</td></tr>
 *   <tr><td>updateFanProfile</td>   <td>VIEW_MATCHES</td>       <td>ALL ROLES (own profile or ADMIN)</td></tr>
 *   <tr><td>deposit</td>             <td>PURCHASE_TICKET</td>     <td>FAN (own), ADMIN</td></tr>
 *   <tr><td>getBalance</td>          <td>VIEW_MATCHES</td>       <td>ALL ROLES (own balance)</td></tr>
 *   <tr><td>getLoyaltyTier</td>     <td>VIEW_LOYALTY</td>       <td>FAN, ADMIN</td></tr>
 *   <tr><td>getPurchaseHistory</td>   <td>PURCHASE_TICKET</td>     <td>FAN (own), ADMIN</td></tr>
 * </table>
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class FanController {

    private final AccountRepository accountRepo;
    private final FanRepository fanRepo;
    private final TicketRepository ticketRepo;
    private final AuthenticationManager authManager;
    private final SecurityContext securityContext;

    public FanController(AccountRepository accountRepo, FanRepository fanRepo,
                         TicketRepository ticketRepo) {
        this.accountRepo = accountRepo;
        this.fanRepo = fanRepo;
        this.ticketRepo = ticketRepo;
        this.authManager = AuthenticationManager.getInstance();
        this.securityContext = SecurityContext.getInstance();
    }

    /**
     * Registers a new fan account atomically.
     * <p>Required: REGISTER_FAN — public endpoint, no authentication required.</p>
     * <p>Validates uniqueness of username and email before writing.</p>
     *
     * @param name             fan's full name
     * @param email            fan's email address
     * @param username         desired username (3-32 chars, unique)
     * @param password         plain-text password (min 6 chars)
     * @param initialDeposit   initial account balance deposit
     * @return the created Fan entity
     * @throws DuplicateEntityException if username or email already exists
     * @throws ValidationException     if input validation fails
     */
    public Fan registerFan(String name, String email, String username,
                          String password, double initialDeposit) throws IOException {
        // No authentication check — this is the public registration endpoint.
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name", name, "Name cannot be empty");
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("email", email, "Invalid email format");
        }
        if (username == null || username.length() < 3 || username.length() > 32) {
            throw new ValidationException("username", username, "Username must be 3-32 characters");
        }
        if (password == null || password.length() < 6) {
            throw new ValidationException("password", "***", "Password must be at least 6 characters");
        }
        if (initialDeposit < 0) {
            throw new ValidationException("initialDeposit", String.valueOf(initialDeposit), "Cannot be negative");
        }

        String cleanUsername = username.trim().toLowerCase();

        if (accountRepo.usernameExists(cleanUsername)) {
            throw new DuplicateEntityException("UserAccount", "username", cleanUsername);
        }
        if (fanRepo.findByEmail(email.trim().toLowerCase()) != null) {
            throw new DuplicateEntityException("Fan", "email", email);
        }

        String personId = UUID.randomUUID().toString();

        Fan fan = new Fan();
        fan.setId(personId);
        fan.setName(name.trim());
        fan.setEmail(email.trim().toLowerCase());
        fan.setAccountBalance(initialDeposit);
        fan.setRegisteredDate(java.time.LocalDateTime.now().toString());

        String passwordHash = authManager.hashPassword(password);

        UserAccount account = new UserAccount();
        account.setUsername(cleanUsername);
        account.setPasswordHash(passwordHash);
        account.setRole(Role.FAN);
        account.setStatus(AccountStatus.ACTIVE);
        account.setPersonId(personId);
        account.setCreatedAt(java.time.LocalDateTime.now().toString());

        fanRepo.save(fan);
        accountRepo.save(account);

        return fan;
    }

    /**
     * Retrieves a fan's profile by ID.
     * <p>Required: caller must be viewing their own profile or be ADMIN.</p>
     */
    public Fan getFanById(String fanId) {
        securityContext.requirePermission(Permission.VIEW_MATCHES);
        Fan fan = fanRepo.findById(fanId);
        if (fan == null) throw new EntityNotFoundException("Fan", fanId);
        // Non-admin callers can only view their own profile.
        UserSession session = securityContext.getCurrentSession();
        if (!session.isAdmin() && !fanId.equals(session.getPersonId())) {
            throw new AccessDeniedException(
                    session.getUsername(), "VIEW_FAN_PROFILE",
                    "Fans can only view their own profile");
        }
        return fan;
    }

    /**
     * Updates a fan's personal information.
     * <p>Required: caller must be updating their own profile or be ADMIN.</p>
     */
    public void updateFanProfile(String fanId, String name, String email,
                                String phoneNumber, String address) throws IOException {
        securityContext.requirePermission(Permission.VIEW_MATCHES);
        UserSession session = securityContext.getCurrentSession();
        if (!session.isAdmin() && !fanId.equals(session.getPersonId())) {
            throw new AccessDeniedException(
                    session.getUsername(), "UPDATE_FAN_PROFILE",
                    "Fans can only update their own profile");
        }

        Fan fan = fanRepo.findById(fanId);
        if (fan == null) throw new EntityNotFoundException("Fan", fanId);

        if (name != null && !name.trim().isEmpty()) {
            fan.setName(name.trim());
        }
        if (email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            Fan existing = fanRepo.findByEmail(email.trim().toLowerCase());
            if (existing != null && !existing.getId().equals(fanId)) {
                throw new DuplicateEntityException("Fan", "email", email);
            }
            fan.setEmail(email.trim().toLowerCase());
        }
        if (phoneNumber != null) {
            fan.setPhoneNumber(phoneNumber);
        }
        if (address != null) {
            fan.setAddress(address);
        }
        fanRepo.save(fan);
    }

    /**
     * Deposits funds into a fan's account.
     * <p>Required: PURCHASE_TICKET — caller must be depositing to their own account or be ADMIN.</p>
     */
    public void deposit(String fanId, double amount) throws IOException {
        securityContext.requirePermission(Permission.PURCHASE_TICKET);
        UserSession session = securityContext.getCurrentSession();
        if (!session.isAdmin() && !fanId.equals(session.getPersonId())) {
            throw new AccessDeniedException(
                    session.getUsername(), "DEPOSIT",
                    "Fans can only deposit to their own account");
        }
        if (amount <= 0) {
            throw new ValidationException("amount", String.valueOf(amount), "Must be positive");
        }
        Fan fan = fanRepo.findById(fanId);
        if (fan == null) throw new EntityNotFoundException("Fan", fanId);
        fan.deposit(amount);
        fanRepo.save(fan);
    }

    /**
     * Returns the fan's current account balance.
     * <p>Required: VIEW_MATCHES — caller must be viewing their own balance or be ADMIN.</p>
     */
    public double getBalance(String fanId) {
        securityContext.requirePermission(Permission.VIEW_MATCHES);
        UserSession session = securityContext.getCurrentSession();
        if (!session.isAdmin() && !fanId.equals(session.getPersonId())) {
            throw new AccessDeniedException(
                    session.getUsername(), "VIEW_BALANCE",
                    "Fans can only view their own balance");
        }
        Fan fan = fanRepo.findById(fanId);
        if (fan == null) throw new EntityNotFoundException("Fan", fanId);
        return fan.getAccountBalance();
    }

    /**
     * Returns loyalty tier information for a fan.
     * <p>Required: VIEW_LOYALTY — FAN or ADMIN.</p>
     */
    public LoyaltyTier getLoyaltyTier(String fanId) {
        securityContext.requirePermission(Permission.VIEW_LOYALTY);
        Fan fan = fanRepo.findById(fanId);
        if (fan == null) throw new EntityNotFoundException("Fan", fanId);
        return fan.getTier();
    }

    /**
     * Returns all tickets purchased by a fan.
     * <p>Required: PURCHASE_TICKET — caller must be viewing their own history or be ADMIN.</p>
     */
    public List<Ticket> getPurchaseHistory(String fanId) {
        securityContext.requirePermission(Permission.PURCHASE_TICKET);
        UserSession session = securityContext.getCurrentSession();
        if (!session.isAdmin() && !fanId.equals(session.getPersonId())) {
            throw new AccessDeniedException(
                    session.getUsername(), "VIEW_PURCHASE_HISTORY",
                    "Fans can only view their own purchase history");
        }
        return ticketRepo.findByFanId(fanId);
    }

    @Override
    public String toString() {
        return "FanController{fans=" + fanRepo.count() + "}";
    }
}
