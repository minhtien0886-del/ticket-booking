package com.club.repository;

import com.club.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete repository for {@link AttendanceRecord} entities backed by attendance_records.csv.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class AttendanceRepository extends GenericCsvRepository<AttendanceRecord> {

    private static final String HEADER = "recordId,matchId,fanId,seatId,entryTime,exitTime,earlyArrival";

    public AttendanceRepository(Path dataDir) {
        super(
            dataDir.resolve("attendance_records.csv"),
            "recordId",
            AttendanceRecord::getRecordId,
            AttendanceRecord::fromCsv,
            AttendanceRecord::toCsv
        );
        setHeaderLine(HEADER);
    }

    public List<AttendanceRecord> findByMatchId(String matchId) {
        return findAll(ar -> matchId.equals(ar.getMatchId()));
    }

    public List<AttendanceRecord> findByFanId(String fanId) {
        return findAll(ar -> fanId.equals(ar.getFanId()));
    }

    public int countByMatchId(String matchId) {
        return findByMatchId(matchId).size();
    }

    @Override
    protected AttendanceRecord parse(String csvLine) {
        return AttendanceRecord.fromCsv(csvLine);
    }

    @Override
    protected String serialize(AttendanceRecord entity) {
        return entity.toCsv();
    }
}
