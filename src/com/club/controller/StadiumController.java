package com.club.controller;

import com.club.model.*;
import com.club.repository.*;
import com.club.security.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Secure stadium seat map controller that renders a dynamic ASCII representation
 * of the stadium layout with color-coded seat states.
 *
 * <h3>Permission Matrix</h3>
 * <table border="1">
 *   <tr><th>Method</th>           <th>Required Permission</th> <th>Who Can Call</th></tr>
 *   <tr><td>renderStadiumMap</td>  <td>VIEW_SEAT_MAP</td>      <td>ALL ROLES</td></tr>
 *   <tr><td>renderSectorMap</td>   <td>VIEW_SEAT_MAP</td>      <td>ALL ROLES</td></tr>
 *   <tr><td>getSectorStats</td>     <td>VIEW_SEAT_MAP</td>      <td>ALL ROLES</td></tr>
 * </table>
 *
 * <p>FAN role has VIEW_SEAT_MAP and can browse the map freely.</p>
 * <p>Anonymous (no session) callers are blocked — authentication is required.</p>
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

    /**
     * Renders the complete stadium seat map to the console.
     * <p>Required: VIEW_SEAT_MAP</p>
     *
     * Legend:
     *   [#] AVAILABLE   [X] BOOKED   [L] LOCKED   [R] RESERVED   [-] UNAVAILABLE
     */
    public void renderStadiumMap() {
        securityContext.requirePermission(Permission.VIEW_SEAT_MAP);
        Map<String, List<Seat>> seatsBySector = new LinkedHashMap<>();
        for (Seat seat : seatRepo.findAll()) {
            seatsBySector.computeIfAbsent(seat.getSector(), k -> new ArrayList<>()).add(seat);
        }
        System.out.println("\n" + repeat("=", 72));
        System.out.println("                        FOOTBALL STADIUM SEAT MAP");
        System.out.println(repeat("=", 72));
        for (Map.Entry<String, List<Seat>> sectorEntry : seatsBySector.entrySet()) {
            String sector = sectorEntry.getKey();
            List<Seat> seats = sectorEntry.getValue();
            System.out.println("\n  [" + sector + "] " + getSectorName(sector)
                    + " " + repeat("-", 50 - sector.length() - getSectorName(sector).length()));
            System.out.println("  " + repeat("-", 60));
            Map<String, List<Seat>> byRow = seats.stream()
                    .collect(Collectors.groupingBy(Seat::getRowNum, LinkedHashMap::new, Collectors.toList()));
            for (Map.Entry<String, List<Seat>> rowEntry : byRow.entrySet()) {
                String row = rowEntry.getKey();
                List<Seat> rowSeats = rowEntry.getValue();
                rowSeats.sort(Comparator.comparingInt(Seat::getSeatNumber));
                System.out.printf("  Row %s: ", row);
                StringBuilder seatLine = new StringBuilder();
                StringBuilder numLine = new StringBuilder("           ");
                for (int i = 0; i < rowSeats.size(); i++) {
                    Seat seat = rowSeats.get(i);
                    String symbol = getSeatSymbol(seat.getStatus());
                    String posNum = String.format("S%02d", i + 1);
                    seatLine.append(symbol).append(posNum).append(" ");
                    numLine.append("       ");
                }
                System.out.println(seatLine.toString());
                System.out.println(numLine.toString());
            }
            printSectorSummary(sector, seats);
        }
        System.out.println("\n" + repeat("=", 72));
        printLegend();
        System.out.println(repeat("=", 72));
    }

    /**
     * Renders a specific sector's seat map.
     * <p>Required: VIEW_SEAT_MAP</p>
     */
    public void renderSectorMap(String sector) {
        securityContext.requirePermission(Permission.VIEW_SEAT_MAP);
        List<Seat> seats = seatRepo.findBySector(sector);
        if (seats.isEmpty()) {
            System.out.println("Sector '" + sector + "' not found.");
            return;
        }
        System.out.println("\n  Sector: " + sector + " (" + getSectorName(sector) + ")");
        System.out.println("  " + repeat("=", 60));
        Map<String, List<Seat>> byRow = seats.stream()
                .collect(Collectors.groupingBy(Seat::getRowNum, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<Seat>> rowEntry : byRow.entrySet()) {
            String row = rowEntry.getKey();
            List<Seat> rowSeats = rowEntry.getValue();
            rowSeats.sort(Comparator.comparingInt(Seat::getSeatNumber));
            System.out.printf("  Row %s: ", row);
            StringBuilder seatLine = new StringBuilder();
            StringBuilder numLine = new StringBuilder("           ");
            for (int i = 0; i < rowSeats.size(); i++) {
                Seat seat = rowSeats.get(i);
                String symbol = getSeatSymbol(seat.getStatus());
                String posNum = String.format("S%02d", i + 1);
                seatLine.append(symbol).append(posNum).append(" ");
                numLine.append("       ");
            }
            System.out.println(seatLine.toString());
            System.out.println(numLine.toString());
        }
        printSectorSummary(sector, seats);
        printLegend();
        System.out.println("  Seat ID Format: SECTOR-ROW-NUMBER  (e.g., VIP-A-012, A-B-015)");
        System.out.println("  Enter position number (S01, S02...) to get the seat ID, or type the full ID.");
        System.out.println(repeat("=", 60));
    }

    /**
     * Returns seat availability statistics for each sector.
     * <p>Required: VIEW_SEAT_MAP</p>
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

    private void printSectorSummary(String sector, List<Seat> seats) {
        long available = seats.stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE).count();
        long booked    = seats.stream().filter(s -> s.getStatus() == SeatStatus.BOOKED).count();
        long locked    = seats.stream().filter(s -> s.getStatus() == SeatStatus.LOCKED).count();
        long reserved  = seats.stream().filter(s -> s.getStatus() == SeatStatus.RESERVED).count();
        double avgPrice = seats.stream().mapToDouble(Seat::getPrice).average().orElse(0);
        System.out.printf("  %s: Available=%d, Booked=%d, Locked=%d, Reserved=%d, AvgPrice=%.2f%n",
                sector, available, booked, locked, reserved, avgPrice);
    }

    private void printLegend() {
        System.out.println("  LEGEND:");
        System.out.println("    [#] AVAILABLE  [X] BOOKED  [L] LOCKED  [R] RESERVED  [-] UNAVAILABLE");
        System.out.println("  SEAT ID FORMAT: SECTOR-ROW-NUMBER  (e.g., VIP-A-012, A-B-015)");
        System.out.println("  POSITION NUMBERS (S01, S02...) shown below each seat help you identify seats.");
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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }

    /** Inner class for per-sector seat statistics. */
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
