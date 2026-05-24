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
public class Ticket {

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

    public String toCsv() {
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

    private String safe(String s) {
        if (s == null) return "";
        return s.contains(",") ? "\"" + s + "\"" : s;
    }

    public static Ticket fromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        Ticket t = new Ticket();
        t.setTicketId(parts.length > 0 ? parts[0] : null);
        t.setMatchId(parts.length > 1 ? parts[1] : null);
        t.setFanId(parts.length > 2 ? parts[2] : null);
        t.setSeatId(parts.length > 3 ? parts[3] : null);
        try { t.setCategory(TicketCategory.valueOf(parts.length > 4 ? parts[4] : "STANDARD")); } catch (Exception e) { /* default */ }
        try { t.setPrice(Double.parseDouble(parts.length > 5 ? parts[5] : "0")); } catch (Exception e) { /* default 0 */ }
        t.setPurchaseDate(parts.length > 6 ? parts[6] : null);
        t.setMatchDate(parts.length > 7 ? parts[7] : null);
        try { t.setCheckedIn(Boolean.parseBoolean(parts.length > 8 ? parts[8] : "false")); } catch (Exception e) { /* default false */ }
        t.setCheckedInTime(parts.length > 9 ? parts[9] : null);
        t.setTransactionId(parts.length > 10 ? parts[10] : null);
        return t;
    }

    private static String[] parseCsvLine(String csv) {
        java.util.List<String> result = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : csv.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
