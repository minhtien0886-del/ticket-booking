package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link AuditLog} entities backed by audit_log.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class AuditLogRepository extends GenericCsvRepository<AuditLog> {

    private static final String HEADER = "logId,timestamp,username,action,resource,resourceId,ipAddress,result,details";

    public AuditLogRepository(Path dataDir) {
        super(
            dataDir.resolve("audit_log.csv"),
            "logId",
            AuditLog::getLogId,
            AuditLog::fromCsv,
            AuditLog::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<AuditLog> findByUsername(String username) {
        return findAll(al -> username.equalsIgnoreCase(al.getUsername()));
    }

    public List<AuditLog> findByAction(String action) {
        return findAll(al -> action.equalsIgnoreCase(al.getAction()));
    }

    public List<AuditLog> findByResource(String resource) {
        return findAll(al -> resource.equalsIgnoreCase(al.getResource()));
    }

    public List<AuditLog> findRecent(int limit) {
        List<AuditLog> all = new ArrayList<>(findAll());
        all.sort((a, b) -> {
            String ta = a.getTimestamp() != null ? a.getTimestamp() : "";
            String tb = b.getTimestamp() != null ? b.getTimestamp() : "";
            return tb.compareTo(ta);
        });
        return all.subList(0, Math.min(limit, all.size()));
    }

    @Override
    protected AuditLog parse(String csvLine) {
        return AuditLog.fromCsv(csvLine);
    }

    @Override
    protected String serialize(AuditLog entity) {
        return entity.toCsv();
    }
}
