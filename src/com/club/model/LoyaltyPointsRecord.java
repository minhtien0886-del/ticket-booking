package com.club.model;

/**
 * Represents a loyalty points record for a fan.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class LoyaltyPointsRecord extends BaseEntity {

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

    @Override
    public String getEntityId() {
        return recordId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
            safe(recordId), safe(fanId), String.valueOf(points),
            safe(transactionType), safe(referenceId), safe(timestamp), safe(description)
        );
    }

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    public static LoyaltyPointsRecord fromCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        if (parts.length < 1) return null;
        LoyaltyPointsRecord lp = new LoyaltyPointsRecord();
        lp.setRecordId(getField(parts, 0, null));
        lp.setFanId(getField(parts, 1, null));
        lp.setPoints(getIntField(parts, 2, 0));
        lp.setTransactionType(getField(parts, 3, null));
        lp.setReferenceId(getField(parts, 4, null));
        lp.setTimestamp(getField(parts, 5, null));
        lp.setDescription(getField(parts, 6, null));
        return lp;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static LoyaltyPointsRecord fromCsv(String csv) {
        return fromCsvLine(csv);
    }

    @Override
    public String toString() {
        return String.format("LoyaltyPointsRecord{fan='%s', points=%d, type='%s'}", fanId, points, transactionType);
    }
}
