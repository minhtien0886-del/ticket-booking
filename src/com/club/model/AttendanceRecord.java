package com.club.model;

/**
 * Represents a match attendance record for audit and analytics.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class AttendanceRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String recordId;
    private String matchId;
    private String fanId;
    private String seatId;
    private String entryTime;
    private String exitTime;
    private boolean earlyArrival;

    public AttendanceRecord() {}

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getFanId() {
        return fanId;
    }

    public void setFanId(String fanId) {
        this.fanId = fanId;
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public String getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(String entryTime) {
        this.entryTime = entryTime;
    }

    public String getExitTime() {
        return exitTime;
    }

    public void setExitTime(String exitTime) {
        this.exitTime = exitTime;
    }

    public boolean isEarlyArrival() {
        return earlyArrival;
    }

    public void setEarlyArrival(boolean earlyArrival) {
        this.earlyArrival = earlyArrival;
    }

    @Override
    public String getEntityId() {
        return recordId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
            safe(recordId), safe(matchId), safe(fanId), safe(seatId),
            safe(entryTime), safe(exitTime), String.valueOf(earlyArrival)
        );
    }

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    public static AttendanceRecord fromCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        if (parts.length < 1) return null;
        AttendanceRecord ar = new AttendanceRecord();
        ar.setRecordId(getField(parts, 0, null));
        ar.setMatchId(getField(parts, 1, null));
        ar.setFanId(getField(parts, 2, null));
        ar.setSeatId(getField(parts, 3, null));
        ar.setEntryTime(getField(parts, 4, null));
        ar.setExitTime(getField(parts, 5, null));
        ar.setEarlyArrival(getBooleanField(parts, 6, false));
        return ar;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static AttendanceRecord fromCsv(String csv) {
        return fromCsvLine(csv);
    }

    @Override
    public String toString() {
        return String.format("AttendanceRecord{id='%s', match='%s', fan='%s'}", recordId, matchId, fanId);
    }
}
