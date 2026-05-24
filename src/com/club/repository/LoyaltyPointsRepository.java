package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link LoyaltyPointsRecord} entities backed by loyalty_points.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class LoyaltyPointsRepository extends GenericCsvRepository<LoyaltyPointsRecord> {

    private static final String HEADER = "recordId,fanId,points,transactionType,referenceId,timestamp,description";

    public LoyaltyPointsRepository(Path dataDir) {
        super(
            dataDir.resolve("loyalty_points.csv"),
            "recordId",
            LoyaltyPointsRecord::getRecordId,
            LoyaltyPointsRecord::fromCsv,
            LoyaltyPointsRecord::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<LoyaltyPointsRecord> findByFanId(String fanId) {
        return findAll(lp -> fanId.equals(lp.getFanId()));
    }

    public List<LoyaltyPointsRecord> findByType(String type) {
        return findAll(lp -> type.equalsIgnoreCase(lp.getTransactionType()));
    }

    @Override
    protected LoyaltyPointsRecord parse(String csvLine) {
        return LoyaltyPointsRecord.fromCsv(csvLine);
    }

    @Override
    protected String serialize(LoyaltyPointsRecord entity) {
        return entity.toCsv();
    }
}
