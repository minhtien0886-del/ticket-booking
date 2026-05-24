package com.club.model;

/**
 * Represents a match attendance record for audit and analytics.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class AttendanceRecord {

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

    public String toCsv() {
        return String.join(",",
            safe(recordId), safe(matchId), safe(fanId), safe(seatId),
            safe(entryTime), safe(exitTime), String.valueOf(earlyArrival)
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.contains(",") ? "\"" + s + "\"" : s;
    }

    public static AttendanceRecord fromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = csv.split(",", -1);
        AttendanceRecord ar = new AttendanceRecord();
        ar.setRecordId(parts.length > 0 ? parts[0] : null);
        ar.setMatchId(parts.length > 1 ? parts[1] : null);
        ar.setFanId(parts.length > 2 ? parts[2] : null);
        ar.setSeatId(parts.length > 3 ? parts[3] : null);
        ar.setEntryTime(parts.length > 4 ? parts[4] : null);
        ar.setExitTime(parts.length > 5 ? parts[5] : null);
        try { ar.setEarlyArrival(Boolean.parseBoolean(parts.length > 6 ? parts[6] : "false")); } catch (Exception e) { /* default false */ }
        return ar;
    }

    @Override
    public String toString() {
        return String.format("AttendanceRecord{id='%s', match='%s', fan='%s'}", recordId, matchId, fanId);
    }
}
