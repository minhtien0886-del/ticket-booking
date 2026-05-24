package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

/**
 * Concrete repository for {@link UserAccount} entities backed by accounts.csv.
 * Manages user account persistence with username uniqueness enforcement.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class AccountRepository extends GenericCsvRepository<UserAccount> {

    private static final String HEADER = "username,passwordHash,role,status,lastLogin,failedLoginAttempts,createdAt,personId";

    private static final AtomicInteger idCounter = new AtomicInteger(1);

    public AccountRepository(Path dataDir) {
        super(
            dataDir.resolve("accounts.csv"),
            "username",
            UserAccount::getUsername,
            UserAccount::fromCsv,
            UserAccount::toCsv
        );
        setHeaderLine(HEADER);
    }

    @Override
    public void save(UserAccount account) throws IOException {
        if (account.getCreatedAt() == null || account.getCreatedAt().isEmpty()) {
            account.setCreatedAt(java.time.LocalDateTime.now().toString());
        }
        super.save(account);
    }

    public UserAccount findByUsername(String username) {
        ensureLoadedQuietly();
        return cache.get(username != null ? username.toLowerCase() : null);
    }

    public boolean usernameExists(String username) {
        return existsById(username != null ? username.toLowerCase() : null);
    }

    public java.util.List<UserAccount> findByRole(Role role) {
        return findAll(a -> a.getRole() == role);
    }

    public java.util.List<UserAccount> findByStatus(AccountStatus status) {
        return findAll(a -> a.getStatus() == status);
    }

    @Override
    protected UserAccount parse(String csvLine) {
        return UserAccount.fromCsv(csvLine);
    }

    @Override
    protected String serialize(UserAccount entity) {
        return entity.toCsv();
    }
}
