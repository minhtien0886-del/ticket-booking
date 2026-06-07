package com.club.controller;

import com.club.model.*;
import com.club.repository.*;
import com.club.security.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Secure controller for stadium seat map operations.
 *
 * <p><strong>MVC Compliance:</strong> This controller contains ZERO direct output calls.
 * All render methods return a {@code String} which the VIEW layer (SecureMenu) is
 * responsible for displaying. No {@code System.out}, {@code System.err}, or
 * {@code Scanner} usage is permitted in this class.</p>
 *
 * <h3>Permission Matrix</h3>
 * <table border="1">
 *   <tr><th>Method</th>           <th>Required Permission</th> <th>Who Can Call</th></tr>
 *   <tr><td>renderStadiumMap</td>  <td>VIEW_SEAT_MAP</td>      <td>ALL ROLES</td></tr>
 *   <tr><td>renderSectorMap</td>   <td>VIEW_SEAT_MAP</td>      <td>ALL ROLES</td></tr>
 *   <tr><td>getSectorStats</td>     <td>VIEW_SEAT_MAP</td>      <td>ALL ROLES</td></tr>
 * </table>
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class StadiumController {

    private final SeatRepository seatRepo;
    private final SecurityContext securityContext;

    public StadiumController(SeatRepository seatRepo) {
        this.seatRepo = seatRepo;
        this.securityContext = SecurityContext.getInstance();
    }

    // -------------------------------------------------------------------------
    // PUBLIC RENDER METHODS — all return String; VIEW layer prints
    // -------------------------------------------------------------------------

    /**
     * Renders the complete stadium seat map as a String.
     *
     * <p>Required: VIEW_SEAT_MAP permission.</p>
     *
     * @return the full ASCII stadium map as a String
     */
    public String renderStadiumMap() {
        securityContext.requirePermission(Permission.VIEW_SEAT_MAP);
        Map<String, List<Seat>> seatsBySector = new LinkedHashMap<>();
        for (Seat seat : seatRepo.findAll()) {
            seatsBySector.computeIfAbsent(seat.getSector(), k -> new ArrayList<>()).add(seat);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(repeat("=", 72)).append("\n");
        sb.append("                        FOOTBALL STADIUM SEAT MAP\n");
        sb.append(repeat("=", 72)).append("\n");

        for (Map.Entry<String, List<Seat>> sectorEntry : seatsBySector.entrySet()) {
            String sector = sectorEntry.getKey();
            List<Seat> seats = sectorEntry.getValue();
            sb.append("\n  [").append(sector).append("] ").append(getSectorName(sector));
            int dashes = 50 - sector.length() - getSectorName(sector).length();
            if (dashes > 0) sb.append(" ").append(repeat("-", dashes));
            sb.append("\n");
            sb.append("  ").append(repeat("-", 60)).append("\n");

            Map<String, List<Seat>> byRow = seats.stream()
                    .collect(Collectors.groupingBy(Seat::getRowNum, LinkedHashMap::new, Collectors.toList()));
            for (Map.Entry<String, List<Seat>> rowEntry : byRow.entrySet()) {
                String row = rowEntry.getKey();
                List<Seat> rowSeats = rowEntry.getValue();
                rowSeats.sort(Comparator.comparingInt(Seat::getSeatNumber));

                sb.append(String.format("  Row %s: ", row));
                StringBuilder seatLine = new StringBuilder();
                StringBuilder numLine = new StringBuilder("           ");

                for (int i = 0; i < rowSeats.size(); i++) {
                    Seat seat = rowSeats.get(i);
                    String symbol = getSeatSymbol(seat.getStatus());
                    String posNum = String.format("S%02d", i + 1);
                    seatLine.append(symbol).append(posNum).append(" ");
                    numLine.append("       ");
                }
                sb.append(seatLine).append("\n");
                sb.append(numLine).append("\n");
            }
            appendSectorSummary(sb, sector, seats);
        }

        sb.append("\n").append(repeat("=", 72)).append("\n");
        appendLegend(sb);
        sb.append(repeat("=", 72)).append("\n");
        return sb.toString();
    }

    /**
     * Renders a specific sector's seat map as a String.
     *
     * <p>Required: VIEW_SEAT_MAP permission.</p>
     *
     * @param sector the sector to render
     * @return the sector map as a String
     */
    public String renderSectorMap(String sector) {
        securityContext.requirePermission(Permission.VIEW_SEAT_MAP);
        List<Seat> seats = seatRepo.findBySector(sector);
        if (seats.isEmpty()) {
            return "Sector '" + sector + "' not found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n  Sector: ").append(sector).append(" (").append(getSectorName(sector)).append(")\n");
        sb.append("  ").append(repeat("=", 60)).append("\n");

        Map<String, List<Seat>> byRow = seats.stream()
                .collect(Collectors.groupingBy(Seat::getRowNum, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<Seat>> rowEntry : byRow.entrySet()) {
            String row = rowEntry.getKey();
            List<Seat> rowSeats = rowEntry.getValue();
            rowSeats.sort(Comparator.comparingInt(Seat::getSeatNumber));
            sb.append(String.format("  Row %s: ", row));
            StringBuilder seatLine = new StringBuilder();
            StringBuilder numLine = new StringBuilder("           ");
            for (int i = 0; i < rowSeats.size(); i++) {
                Seat seat = rowSeats.get(i);
                String symbol = getSeatSymbol(seat.getStatus());
                String posNum = String.format("S%02d", i + 1);
                seatLine.append(symbol).append(posNum).append(" ");
                numLine.append("       ");
            }
            sb.append(seatLine).append("\n");
            sb.append(numLine).append("\n");
        }

        appendSectorSummary(sb, sector, seats);
        appendLegend(sb);
        sb.append("  Seat ID Format: SECTOR-ROW-NUMBER  (e.g., VIP-A-012, A-B-015)%n");
        sb.append("  Enter position number (S01, S02...) to get the seat ID, or type the full ID.%n");
        sb.append(repeat("=", 60)).append("\n");
        return sb.toString();
    }

    /**
     * Returns seat availability statistics for each sector.
     *
     * <p>Required: VIEW_SEAT_MAP permission.</p>
     *
     * @return map of sector name to statistics
     */
    public Map<String, SectorStats> getSectorStats() {
        securityContext.requirePermission(Permission.VIEW_SEAT_MAP);
        Map<String, SectorStats> stats = new LinkedHashMap<>();
        for (Seat seat : seatRepo.findAll()) {
            String sector = seat.getSector();
            stats.computeIfAbsent(sector, k -> new SectorStats(sector))
                    .addSeat(seat.getStatus());
        }
        return stats;
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    private void appendSectorSummary(StringBuilder sb, String sector, List<Seat> seats) {
        long available = seats.stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE).count();
        long booked    = seats.stream().filter(s -> s.getStatus() == SeatStatus.BOOKED).count();
        long locked    = seats.stream().filter(s -> s.getStatus() == SeatStatus.LOCKED).count();
        long reserved  = seats.stream().filter(s -> s.getStatus() == SeatStatus.RESERVED).count();
        double avgPrice = seats.stream().mapToDouble(Seat::getPrice).average().orElse(0);
        sb.append(String.format("  %s: Available=%d, Booked=%d, Locked=%d, Reserved=%d, AvgPrice=%.2f%n",
                sector, available, booked, locked, reserved, avgPrice));
    }

    private void appendLegend(StringBuilder sb) {
        sb.append("  LEGEND:\n");
        sb.append("    [#] AVAILABLE  [X] BOOKED  [L] LOCKED  [R] RESERVED  [-] UNAVAILABLE\n");
        sb.append("  SEAT ID FORMAT: SECTOR-ROW-NUMBER  (e.g., VIP-A-012, A-B-015)%n");
        sb.append("  POSITION NUMBERS (S01, S02...) shown below each seat help you identify seats.%n");
    }

    private String getSeatSymbol(SeatStatus status) {
        switch (status) {
            case AVAILABLE:  return "[#]";
            case BOOKED:     return "[X]";
            case LOCKED:     return "[L]";
            case RESERVED:   return "[R]";
            case UNAVAILABLE:return "[-]";
            default:         return "[?]";
        }
    }

    private String getSectorName(String sector) {
        switch (sector != null ? sector.toUpperCase() : "") {
            case "VIP":   return "VIP Box";
            case "A":    return "Main Stand";
            case "B":    return "East Wing";
            case "C":    return "West Wing";
            case "D":    return "South End";
            case "E":    return "North End";
            case "F":    return "Family Section";
            default:      return "General";
        }
    }

    private String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(count * s.length());
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // INNER CLASS — Sector statistics
    // -------------------------------------------------------------------------

    /** Per-sector seat statistics. */
    public static class SectorStats {
        public final String sector;
        public int total;
        public int available;
        public int booked;
        public int locked;
        public int reserved;
        public int unavailable;

        public SectorStats(String sector) { this.sector = sector; }

        public void addSeat(SeatStatus status) {
            total++;
            switch (status) {
                case AVAILABLE:  available++;   break;
                case BOOKED:    booked++;     break;
                case LOCKED:    locked++;     break;
                case RESERVED:  reserved++;   break;
                case UNAVAILABLE:unavailable++; break;
            }
        }

        public double getOccupancyRate() {
            return total > 0 ? (double) (booked + locked + reserved) / total * 100 : 0;
        }
    }

    @Override
    public String toString() {
        return "StadiumController{seats=" + seatRepo.count() + "}";
    }
}
