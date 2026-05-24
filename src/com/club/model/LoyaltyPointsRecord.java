package com.club.model;

/**
 * Represents a loyalty points record for a fan.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class LoyaltyPointsRecord {

    private static final long serialVersionUID = 1L;

    private String recordId;
    private String fanId;
    private int points;
    private String transactionType;
    private String referenceId;
    private String timestamp;
    private String description;

    public LoyaltyPointsRecord() {}

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getFanId() {
        return fanId;
    }

    public void setFanId(String fanId) {
        this.fanId = fanId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String toCsv() {
        return String.join(",",
            safe(recordId), safe(fanId), String.valueOf(points),
            safe(transactionType), safe(referenceId), safe(timestamp), safe(description)
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.contains(",") ? "\"" + s + "\"" : s;
    }

    public static LoyaltyPointsRecord fromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = csv.split(",", -1);
        LoyaltyPointsRecord lp = new LoyaltyPointsRecord();
        lp.setRecordId(parts.length > 0 ? parts[0] : null);
        lp.setFanId(parts.length > 1 ? parts[1] : null);
        try { lp.setPoints(Integer.parseInt(parts.length > 2 ? parts[2] : "0")); } catch (Exception e) { /* default 0 */ }
        lp.setTransactionType(parts.length > 3 ? parts[3] : null);
        lp.setReferenceId(parts.length > 4 ? parts[4] : null);
        lp.setTimestamp(parts.length > 5 ? parts[5] : null);
        lp.setDescription(parts.length > 6 ? parts[6] : null);
        return lp;
    }

    @Override
    public String toString() {
        return String.format("LoyaltyPointsRecord{fan='%s', points=%d, type='%s'}", fanId, points, transactionType);
    }
}
