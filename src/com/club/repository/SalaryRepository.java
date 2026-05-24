package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link SalaryRecord} entities backed by salaries.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class SalaryRepository extends GenericCsvRepository<SalaryRecord> {

    private static final String HEADER = "recordId,personId,personType,grossSalary,deductions,netSalary,payPeriod,paymentDate,processedBy,status";

    public SalaryRepository(Path dataDir) {
        super(
            dataDir.resolve("salaries.csv"),
            "recordId",
            SalaryRecord::getRecordId,
            SalaryRecord::fromCsv,
            SalaryRecord::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<SalaryRecord> findByPersonId(String personId) {
        return findAll(sr -> personId.equals(sr.getPersonId()));
    }

    public List<SalaryRecord> findByPayPeriod(String period) {
        return findAll(sr -> period.equals(sr.getPayPeriod()));
    }

    public List<SalaryRecord> findByStatus(String status) {
        return findAll(sr -> status.equalsIgnoreCase(sr.getStatus()));
    }

    @Override
    protected SalaryRecord parse(String csvLine) {
        return SalaryRecord.fromCsv(csvLine);
    }

    @Override
    protected String serialize(SalaryRecord entity) {
        return entity.toCsv();
    }
}
