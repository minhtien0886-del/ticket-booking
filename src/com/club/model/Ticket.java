package com.club.model;

import java.util.Objects;

/**
 * Represents a ticket for a football match issued to a fan.
 * Each ticket is linked to a specific seat and match, and tracks
 * the entire lifecycle from purchase to admission.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class Ticket extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String ticketId;
    private String matchId;
    private String fanId;
    private String seatId;
    private TicketCategory category;
    private double price;
    private String purchaseDate;
    private String matchDate;
    private boolean checkedIn;
    private String checkedInTime;
    private String transactionId;

    public Ticket() {
        this.checkedIn = false;
    }

    public Ticket(String ticketId, String matchId, String fanId, String seatId,
                  TicketCategory category, double price) {
        this.ticketId = ticketId;
        this.matchId = matchId;
        this.fanId = fanId;
        this.seatId = seatId;
        this.category = category;
        this.price = price;
        this.checkedIn = false;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
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

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(String matchDate) {
        this.matchDate = matchDate;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public String getCheckedInTime() {
        return checkedInTime;
    }

    public void setCheckedInTime(String checkedInTime) {
        this.checkedInTime = checkedInTime;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void checkIn(String timestamp) {
        if (!this.checkedIn) {
            this.checkedIn = true;
            this.checkedInTime = timestamp;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticket ticket = (Ticket) o;
        return Objects.equals(ticketId, ticket.ticketId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketId);
    }

    @Override
    public String toString() {
        return String.format(
            "Ticket{id='%s', match='%s', fan='%s', seat='%s', price=%.2f, checkedIn=%s}",
            ticketId, matchId, fanId, seatId, price, checkedIn
        );
    }

    @Override
    public String getEntityId() {
        return ticketId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
            safe(ticketId),
            safe(matchId),
            safe(fanId),
            safe(seatId),
            category != null ? category.name() : "STANDARD",
            String.valueOf(price),
            safe(purchaseDate),
            safe(matchDate),
            String.valueOf(checkedIn),
            safe(checkedInTime),
            safe(transactionId)
        );
    }

    /** @deprecated Use {@link #toCsvLine()} instead. */
    @Deprecated
    public String toCsv() {
        return toCsvLine();
    }

    public static Ticket fromCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        if (parts.length < 1) return null;
        Ticket t = new Ticket();
        t.setTicketId(getField(parts, 0, null));
        t.setMatchId(getField(parts, 1, null));
        t.setFanId(getField(parts, 2, null));
        t.setSeatId(getField(parts, 3, null));
        try { t.setCategory(TicketCategory.valueOf(getField(parts, 4, "STANDARD"))); } catch (Exception e) { /* default */ }
        t.setPrice(getDoubleField(parts, 5, 0.0));
        t.setPurchaseDate(getField(parts, 6, null));
        t.setMatchDate(getField(parts, 7, null));
        t.setCheckedIn(getBooleanField(parts, 8, false));
        t.setCheckedInTime(getField(parts, 9, null));
        t.setTransactionId(getField(parts, 10, null));
        return t;
    }

    /** @deprecated Use {@link #fromCsvLine(String)} instead. */
    @Deprecated
    public static Ticket fromCsv(String csv) {
        return fromCsvLine(csv);
    }

    // parseCsvLine() and safe() are inherited from BaseEntity
}
