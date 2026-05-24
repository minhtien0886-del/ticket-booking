package com.club.service;

import com.club.model.*;
import com.club.repository.*;
import com.club.exception.*;
import java.io.IOException;
import java.util.*;

/**
 * Human resources management service for players and staff.
 * Handles personnel records, salary management, and fitness tracking.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class HumanResourceService {

    private final PlayerRepository playerRepo;
    private final StaffRepository staffRepo;
    private final SalaryRepository salaryRepo;

    public HumanResourceService(PlayerRepository playerRepo,
                               StaffRepository staffRepo,
                               SalaryRepository salaryRepo) {
        this.playerRepo = playerRepo;
        this.staffRepo = staffRepo;
        this.salaryRepo = salaryRepo;
    }

    // ============ PLAYER OPERATIONS ============

    public List<Player> getAllPlayers() {
        return new ArrayList<>(playerRepo.findAll());
    }

    public Player getPlayer(String playerId) {
        return playerRepo.findById(playerId);
    }

    public List<Player> getPlayersByPosition(Position position) {
        return playerRepo.findByPosition(position);
    }

    public List<Player> getHealthyPlayers() {
        return playerRepo.findHealthyPlayers();
    }

    public List<Player> getTopScorers(int limit) {
        return playerRepo.findTopScorers(limit);
    }

    public void updatePlayerFitness(String playerId, int newFitness) throws IOException {
        playerRepo.updateFitness(playerId, newFitness);
    }

    public void recordPlayerGoal(String playerId, int goals) throws IOException {
        Player p = playerRepo.findById(playerId);
        if (p != null) {
            p.recordGoal(goals);
            playerRepo.save(p);
        }
    }

    // ============ STAFF OPERATIONS ============

    public List<Staff> getAllStaff() {
        return new ArrayList<>(staffRepo.findAll());
    }

    public Staff getStaff(String staffId) {
        return staffRepo.findById(staffId);
    }

    public List<Staff> getStaffByDepartment(String department) {
        return staffRepo.findByDepartment(department);
    }

    public List<Staff> getActiveStaff() {
        return staffRepo.findActiveStaff();
    }

    public double getTotalSalaryBill() {
        return staffRepo.getTotalSalaryBill();
    }

    public double getTotalPayroll() {
        double staffSalaries = staffRepo.getTotalSalaryBill();
        double playerSalaries = playerRepo.findAll().stream()
            .mapToDouble(Player::getSalary).sum();
        return staffSalaries + playerSalaries;
    }

    // ============ SALARY OPERATIONS ============

    public void processSalaryPayment(String personId, String personType,
                                     double grossSalary, String period,
                                     String processedBy) throws IOException {
        double deduction = grossSalary * 0.1;
        double netSalary = grossSalary - deduction;

        SalaryRecord record = new SalaryRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setPersonId(personId);
        record.setPersonType(personType);
        record.setGrossSalary(grossSalary);
        record.setDeductions(deduction);
        record.setNetSalary(netSalary);
        record.setPayPeriod(period);
        record.setPaymentDate(java.time.LocalDateTime.now().toString());
        record.setProcessedBy(processedBy);
        record.setStatus("PAID");

        salaryRepo.save(record);
    }

    public List<SalaryRecord> getSalaryHistory(String personId) {
        return salaryRepo.findByPersonId(personId);
    }

    // ============ REPORTS ============

    /**
     * Generates a comprehensive personnel report by aggregating player and staff
     * rosters from their respective CSV repositories in real-time.
     *
     * <h4>Aggregation Logic</h4>
     * <ul>
     *   <li>Streams all players from {@link PlayerRepository#findAll()}.</li>
     *   <li>Groups by {@link Position} using a map (handles positions with zero players).</li>
     *   <li>Streams all staff from {@link StaffRepository#findAll()}.</li>
     *   <li>Groups by {@code department} field (null-safe with "UNKNOWN" fallback).</li>
     *   <li>Computes total monthly payroll: player salaries + staff salaries.</li>
     * </ul>
     *
     * <h4>Time Complexity</h4>
     * <p>O(P + S) where P = player count, S = staff count.
     * Each entity is processed exactly once in a linear scan.</p>
     *
     * @return a {@link PersonnelReport} with all aggregated roster data
     */
    public PersonnelReport generatePersonnelReport() {
        int totalPlayers = playerRepo.count();
        int totalStaff = staffRepo.count();
        int healthyPlayers = playerRepo.findHealthyPlayers().size();

        // Group players by tactical position.
        Map<Position, Integer> playersByPosition = new LinkedHashMap<>();
        for (Position pos : Position.values()) {
            playersByPosition.put(pos, 0);
        }
        for (Player p : playerRepo.findAll()) {
            Position pos = p.getPosition();
            if (pos != null) {
                playersByPosition.merge(pos, 1, Integer::sum);
            }
        }

        // Group staff by organisational department.
        Map<String, Integer> staffByDepartment = new LinkedHashMap<>();
        for (Staff s : staffRepo.findAll()) {
            String dept = s.getDepartment() != null ? s.getDepartment() : "UNKNOWN";
            staffByDepartment.merge(dept, 1, Integer::sum);
        }

        // Compute total monthly payroll.
        double totalPayroll = getTotalPayroll();

        // Compute payroll breakdown.
        double playerTotal = playerRepo.findAll().stream()
                .mapToDouble(Player::getSalary).sum();
        double staffTotal = staffRepo.findAll().stream()
                .mapToDouble(Staff::getSalary).sum();

        return new PersonnelReport(totalPlayers, totalStaff, healthyPlayers,
                totalPayroll, playerTotal, staffTotal,
                playersByPosition, staffByDepartment);
    }

    /**
     * Hires a new player and persists to the repository.
     *
     * @param player the player to hire (ID generated if null)
     */
    public void hirePlayer(Player player) throws IOException {
        if (player == null) throw new IllegalArgumentException("Player cannot be null");
        if (player.getId() == null || player.getId().isEmpty()) {
            player.setId(UUID.randomUUID().toString());
        }
        playerRepo.save(player);
    }

    /**
     * Hires a new staff member and persists to the repository.
     *
     * @param staff the staff member to hire (ID generated if null)
     */
    public void hireStaff(Staff staff) throws IOException {
        if (staff == null) throw new IllegalArgumentException("Staff cannot be null");
        if (staff.getId() == null || staff.getId().isEmpty()) {
            staff.setId(UUID.randomUUID().toString());
        }
        staffRepo.save(staff);
    }

    /**
     * Terminates a player by marking them as inactive via InjuryStatus.
     * In this model, "terminated" means the player is no longer selectable —
     * achieved by setting InjuryStatus to UNDER_REVIEW (non-Hockey).
     *
     * @param playerId the player's ID
     */
    public void terminatePlayer(String playerId) throws IOException {
        Player p = playerRepo.findById(playerId);
        if (p == null) return;
        p.setInjuryStatus(InjuryStatus.UNDER_REVIEW);
        playerRepo.save(p);
    }

    /**
     * Terminates a staff member by marking them as inactive.
     *
     * @param staffId the staff member's ID
     */
    public void terminateStaff(String staffId) throws IOException {
        Staff s = staffRepo.findById(staffId);
        if (s == null) return;
        s.setActive(false);
        staffRepo.save(s);
    }

    /**
     * Immutable personnel report with styled ASCII output.
     *
     * <p>Contains aggregated roster data and payroll totals.</p>
     */
    public static class PersonnelReport {

        public final int totalPlayers;
        public final int totalStaff;
        public final int healthyPlayers;
        public final double totalPayroll;
        public final double playerPayroll;
        public final double staffPayroll;
        public final Map<Position, Integer> playersByPosition;
        public final Map<String, Integer> staffByDepartment;

        public PersonnelReport(int totalPlayers, int totalStaff, int healthyPlayers,
                              double totalPayroll, double playerPayroll, double staffPayroll,
                              Map<Position, Integer> playersByPosition,
                              Map<String, Integer> staffByDepartment) {
            this.totalPlayers = totalPlayers;
            this.totalStaff = totalStaff;
            this.healthyPlayers = healthyPlayers;
            this.totalPayroll = totalPayroll;
            this.playerPayroll = playerPayroll;
            this.staffPayroll = staffPayroll;
            this.playersByPosition = playersByPosition;
            this.staffByDepartment = staffByDepartment;
        }

        /**
         * Produces a styled multi-section ASCII personnel report.
         *
         * @return formatted report string
         */
        public String toFormattedReport() {
            StringBuilder sb = new StringBuilder();
            String sep72  = repeat("=", 72);
            String sep50  = repeat("-", 50);
            String sep72n = sep72 + "\n";

            // -- Header
            sb.append(sep72n);
            sb.append("                 PERSONNEL ROSTER REPORT\n");
            sb.append("                 Generated: ")
                    .append(java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .append("\n");
            sb.append(sep72n);

            // -- Headcount Summary
            sb.append("  HEADCOUNT SUMMARY\n");
            sb.append(sep50).append("\n");
            sb.append(String.format("  %-35s %10d%n", "  Total Players:", totalPlayers));
            sb.append(String.format("  %-35s %10d%n", "  Healthy Players:", healthyPlayers));
            sb.append(String.format("  %-35s %10d%n", "  Injured/Suspended:",
                    totalPlayers - healthyPlayers));
            sb.append(String.format("  %-35s %10d%n", "  Total Staff:", totalStaff));
            sb.append(sep72n);

            // -- Payroll Summary
            sb.append("  MONTHLY PAYROLL\n");
            sb.append(sep50).append("\n");
            sb.append(String.format("  %-35s %15.2f%n", "  Player Payroll:", playerPayroll));
            sb.append(String.format("  %-35s %15.2f%n", "  Staff Payroll:", staffPayroll));
            sb.append(String.format("  %-35s %15.2f%n", "  TOTAL MONTHLY PAYROLL:", totalPayroll));
            sb.append(sep72n);

            // -- Players by Position
            sb.append("  PLAYERS BY POSITION\n");
            sb.append(sep50).append("\n");
            for (Map.Entry<Position, Integer> e : playersByPosition.entrySet()) {
                String posName = e.getKey().name().replace("_", " ");
                sb.append(String.format("  %-35s %10d%n",
                        "  " + posName + ":", e.getValue()));
            }
            sb.append(String.format("  %-35s %10d%n", "  TOTAL SQUAD:", totalPlayers));
            sb.append(sep72n);

            // -- Staff by Department
            if (!staffByDepartment.isEmpty()) {
                sb.append("  STAFF BY DEPARTMENT\n");
                sb.append(sep50).append("\n");
                for (Map.Entry<String, Integer> e : staffByDepartment.entrySet()) {
                    sb.append(String.format("  %-35s %10d%n",
                            "  " + e.getKey() + ":", e.getValue()));
                }
                sb.append(String.format("  %-35s %10d%n", "  TOTAL STAFF:", totalStaff));
                sb.append(sep72n);
            }

            return sb.toString();
        }

        private static String repeat(String s, int count) {
            StringBuilder sb = new StringBuilder(count * s.length());
            for (int i = 0; i < count; i++) sb.append(s);
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return String.format("HumanResourceService{players=%d, staff=%d}",
            playerRepo.count(), staffRepo.count());
    }
}
