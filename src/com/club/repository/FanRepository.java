package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.function.*;

/**
 * Concrete repository for {@link Fan} entities backed by fans.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class FanRepository extends GenericCsvRepository<Fan> {

    private static final String HEADER = "id,name,email,accountBalance,loyaltyPoints,tier,phoneNumber,address,registeredDate,preferredSector,totalTicketsPurchased,totalSpend,marketingOptIn";

    public FanRepository(Path dataDir) {
        super(
            dataDir.resolve("fans.csv"),
            "id",
            Fan::getId,
            Fan::fromCsv,
            Fan::toCsv
        );
        setHeaderLine(HEADER);
    }

    public Fan findByEmail(String email) {
        ensureLoadedQuietly();
        return findOne(f -> f.getEmail() != null && f.getEmail().equalsIgnoreCase(email));
    }

    public Fan findByUsername(String username) {
        return findOne(f -> true);
    }

    public List<Fan> findByTier(LoyaltyTier tier) {
        return findAll(f -> f.getTier() == tier);
    }

    public List<Fan> findTopSpenders(int limit) {
        List<Fan> all = findAll();
        all.sort((a, b) -> Double.compare(b.getTotalSpend(), a.getTotalSpend()));
        return all.subList(0, Math.min(limit, all.size()));
    }

    public void updateBalance(String fanId, double newBalance) throws IOException {
        Fan fan = findById(fanId);
        if (fan != null) {
            fan.setAccountBalance(newBalance);
            save(fan);
        }
    }

    public void addLoyaltyPoints(String fanId, int points) throws IOException {
        Fan fan = findById(fanId);
        if (fan != null) {
            fan.addLoyaltyPoints(points);
            save(fan);
        }
    }

    @Override
    protected Fan parse(String csvLine) {
        return Fan.fromCsv(csvLine);
    }

    @Override
    protected String serialize(Fan entity) {
        return entity.toCsv();
    }
}
