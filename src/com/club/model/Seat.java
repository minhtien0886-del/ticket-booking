package com.club.model;

import java.util.Objects;

/**
 * Represents a physical seat within the football stadium.
 * Each seat has a unique identifier, sector classification, pricing tier,
 * and a state machine-driven status. Seats support optimistic locking via
 * a version counter to prevent concurrent modification conflicts.
 *
 * <p>The seat ID follows a strict format: SECTOR-ROW-NUMBER (e.g., VIP-A-012).</p>
 * <p>Seat state transitions: AVAILABLE -> LOCKED -> BOOKED -> AVAILABLE (via cancellation)</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 * @see SeatStatus
 */
public class Seat {

    private static final long serialVersionUID = 1L;

    /** Unique seat identifier in format SECTOR-ROW-NUMBER (e.g., VIP-A-012). */
    private String seatId;

    /** Stadium sector designation (VIP, A, B, C, D, E). */
    private String sector;

    /** Row identifier within the sector (A, B, C, ..., Z). */
    private String rowNum;

    /** Seat number within the row (1-99). */
    private int seatNumber;

    /** Base price for this seat in currency units. */
    private double price;

    /** Current operational status per the state machine. */
    private SeatStatus status;

    /** Version counter for optimistic locking — increments on every update. */
    private int version;

    /** Whether this seat has a clear sightline of the pitch. */
    private boolean hasGoodView;

    /** Whether this seat is covered (has a roof overhead). */
    private boolean isCovered;

    /** Additional description (e.g., wheelchair accessible). */
    private String description;

    /**
     * Default constructor.
     */
    public Seat() {
        this.status = SeatStatus.AVAILABLE;
        this.version = 1;
    }

    /**
     * Full constructor.
     */
    public Seat(String seatId, String sector, String rowNum, int seatNumber,
                double price, SeatStatus status) {
        setSeatId(seatId);
        this.sector = sector;
        this.rowNum = rowNum;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = status != null ? status : SeatStatus.AVAILABLE;
        this.version = 1;
    }

    // ============ GETTERS AND SETTERS ============

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        if (seatId == null || seatId.trim().isEmpty()) {
            throw new IllegalArgumentException("Seat ID cannot be null or empty");
        }
        if (!seatId.matches("^[A-Z]+-[A-Z]-\\d{3,4}$")) {
            throw new IllegalArgumentException(
                "Seat ID must match format SECTOR-ROW-NUMBER (e.g., VIP-A-012): " + seatId
            );
        }
        this.seatId = seatId.toUpperCase();
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector != null ? sector.toUpperCase() : "UNKNOWN";
    }

    public String getRowNum() {
        return rowNum;
    }

    public void setRowNum(String rowNum) {
        this.rowNum = rowNum != null ? rowNum.toUpperCase() : "A";
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        if (seatNumber < 1 || seatNumber > 9999) {
            throw new IllegalArgumentException("Seat number must be between 1 and 9999");
        }
        this.seatNumber = seatNumber;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        if (price < 0) {
            throw new IllegalArgumentException("Seat price cannot be negative");
        }
        this.price = price;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Seat status cannot be null");
        }
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        if (version < 1) {
            throw new IllegalArgumentException("Version must be >= 1");
        }
        this.version = version;
    }

    public boolean isHasGoodView() {
        return hasGoodView;
    }

    public void setHasGoodView(boolean hasGoodView) {
        this.hasGoodView = hasGoodView;
    }

    public boolean isCovered() {
        return isCovered;
    }

    public void setCovered(boolean covered) {
        isCovered = covered;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // ============ STATE MACHINE OPERATIONS ============

    /**
     * Transitions the seat from AVAILABLE to LOCKED state.
     *
     * @return true if transition succeeded, false if seat was not available
     */
    public boolean lock() {
        if (this.status == SeatStatus.AVAILABLE) {
            this.status = SeatStatus.LOCKED;
            this.version++;
            return true;
        }
        return false;
    }

    /**
     * Transitions the seat from LOCKED to BOOKED state.
     *
     * @return true if transition succeeded, false if seat was not locked
     */
    public boolean book() {
        if (this.status == SeatStatus.LOCKED) {
            this.status = SeatStatus.BOOKED;
            this.version++;
            return true;
        }
        return false;
    }

    /**
     * Releases the seat back to AVAILABLE state (cancellation).
     *
     * @return true if transition succeeded, false otherwise
     */
    public boolean release() {
        if (this.status == SeatStatus.LOCKED || this.status == SeatStatus.BOOKED) {
            this.status = SeatStatus.AVAILABLE;
            this.version++;
            return true;
        }
        return false;
    }

    /**
     * Attempts an optimistic lock by verifying the expected version.
     *
     * @param expectedVersion the version the caller read previously
     * @return true if versions match and update is safe, false if version mismatch
     */
    public boolean optimisticLock(int expectedVersion) {
        if (this.version == expectedVersion) {
            this.version++;
            return true;
        }
        return false;
    }

    /**
     * Checks whether this seat can be booked in its current state.
     *
     * @return true if the seat is bookable
     */
    public boolean isBookable() {
        return status == SeatStatus.AVAILABLE && status.isBookable();
    }

    /**
     * Parses the sector from the seat ID.
     *
     * @return the sector portion of the seat ID
     */
    public String extractSector() {
        if (seatId != null && seatId.contains("-")) {
            return seatId.split("-")[0];
        }
        return sector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seat seat = (Seat) o;
        return Objects.equals(seatId, seat.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seatId);
    }

    @Override
    public String toString() {
        return String.format(
            "Seat{id='%s', sector=%s, row=%s, num=%d, price=%.2f, status=%s, version=%d}",
            seatId, sector, rowNum, seatNumber, price, status, version
        );
    }

    public String toCsv() {
        return String.join(",",
            safe(seatId),
            safe(sector),
            safe(rowNum),
            String.valueOf(seatNumber),
            String.valueOf(price),
            status != null ? status.name() : "AVAILABLE",
            String.valueOf(version),
            String.valueOf(hasGoodView),
            String.valueOf(isCovered),
            safe(description)
        );
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.contains(",") ? "\"" + s + "\"" : s;
    }

    public static Seat fromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = parseCsvLine(csv);
        Seat s = new Seat();
        s.setSeatId(parts.length > 0 ? parts[0] : "");
        s.setSector(parts.length > 1 ? parts[1] : null);
        s.setRowNum(parts.length > 2 ? parts[2] : null);
        try { s.setSeatNumber(Integer.parseInt(parts.length > 3 ? parts[3] : "1")); } catch (Exception e) { /* default 1 */ }
        try { s.setPrice(Double.parseDouble(parts.length > 4 ? parts[4] : "0")); } catch (Exception e) { /* default 0 */ }
        try { s.setStatus(SeatStatus.valueOf(parts.length > 5 ? parts[5] : "AVAILABLE")); } catch (Exception e) { /* default */ }
        try { s.setVersion(Integer.parseInt(parts.length > 6 ? parts[6] : "1")); } catch (Exception e) { /* default 1 */ }
        try { s.setHasGoodView(Boolean.parseBoolean(parts.length > 7 ? parts[7] : "true")); } catch (Exception e) { /* default true */ }
        try { s.setCovered(Boolean.parseBoolean(parts.length > 8 ? parts[8] : "false")); } catch (Exception e) { /* default false */ }
        s.setDescription(parts.length > 9 ? parts[9] : null);
        return s;
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
