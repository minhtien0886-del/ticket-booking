package com.club.util;

import com.club.model.*;
import com.club.repository.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.Base64;

/**
 * Automatic data generator that populates all 16 CSV files with 10,000+ valid,
 * non-colliding sample records upon initialization.
 *
 * <p>Generates realistic data for:</p>
 * <ul>
 *   <li>100+ players with realistic names and stats</li>
 *   <li>50+ staff members across departments</li>
 *   <li>200+ fans with loyalty tier distribution</li>
 *   <li>6,000+ stadium seats across 6 sectors</li>
 *   <li>1,000+ tickets</li>
 *   <li>500+ transactions</li>
 *   <li>100+ merchandise products</li>
 *   <li>50+ matches</li>
 *   <li>League table entries</li>
 *   <li>Lineups, attendance, loyalty records, salary records</li>
 * </ul>
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class DataGenerator {

    private static final Random rand = new Random(42);
    private static final String[] FIRST_NAMES = {
        "Nguyen", "Tran", "Le", "Pham", "Hoang", "Bui", "Do", "Dang", "Vu", "Luong",
        "John", "David", "Michael", "James", "Robert", "William", "Thomas", "Carlos", "Luis",
        "Marco", "Antonio", "Pedro", "Mohammed", "Ahmed", "Yuki", "Kenji", "Park", "Kim", "Lee"
    };
    private static final String[] LAST_NAMES = {
        "Van Minh", "Van Duc", "Van Khanh", "Van Hieu", "Van Sang",
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Martinez", "Rodriguez",
        "Silva", "Santos", "Oliveira", "Costa", "Takahashi", "Tanaka", "Kim", "Park", "Choi"
    };
    private static final String[] TEAM_NAMES = {
        "Home FC", "Away United", "City Warriors", "Royal Sporting", "Olympic Club",
        "Athletic FC", "United Stars", "Rovers FC", "Wanderers FC", "Athletico Club",
        "Real United", "Sporting FC", "Galaxy FC", "Dynasty FC", "Thunder FC"
    };
    private static final String[] COMPETITIONS = {
        "Premier League", "National Cup", "Super Cup", "Champions League",
        "League Cup", "Friendly Tournament"
    };
    private static final String[] STADIUM_SECTORS = {"VIP", "A", "B", "C", "D", "E", "F"};
    private static final String[] PRODUCT_NAMES = {
        "Home Jersey 2026", "Away Jersey 2026", "Third Kit 2026",
        "Club Scarf", "Club Cap", "Club Jacket", "Training Shirt",
        "Official Match Ball", "Club Flag", "Club Mug", "Keychain Set",
        "Home Shorts", "Training Pants", "Stadium Blanket", "Signed Jersey"
    };

    private final Path dataDir;
    private int recordsGenerated = 0;

    public DataGenerator(Path dataDir) {
        this.dataDir = dataDir;
    }

    public static Path getDefaultDataDir() {
        return Paths.get("data");
    }

    /**
     * Checks whether data files need to be generated (all empty).
     */
    public boolean needsGeneration() {
        File dir = dataDir.toFile();
        if (!dir.exists()) return true;

        File[] csvs = dir.listFiles((d, name) -> name.endsWith(".csv"));
        if (csvs == null || csvs.length == 0) return true;

        for (File csv : csvs) {
            if (csv.length() > 10) return false;
        }
        return true;
    }

    /**
     * Checks whether the merchandise catalog needs to be seeded.
     * Returns true if merchandise.csv is missing or has zero content.
     */
    private boolean needsMerchandiseSeed() {
        Path f = dataDir.resolve("merchandise.csv");
        if (!Files.exists(f)) return true;
        try {
            long size = Files.size(f);
            if (size == 0) return true;
            // Check if header-only (no data rows)
            List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
            return lines.size() <= 1;
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * Seeds the merchandise catalog with the official product lineup.
     *
     * <p>Only executes when merchandise.csv is absent or empty, preserving any
     * existing purchase history in the file. Writes using UTF-8 encoding
     * via the Write-Ahead Log for crash-safety.</p>
     *
     * <p>The 5-column schema written matches {@code Merchandise.toCsv()}:</p>
     * <pre>productId,name,description,category,basePrice,size,color,stockQuantity,active,imageUrl</pre>
     */
    private void doSeedMerchandiseIfEmpty() throws IOException {
        if (!needsMerchandiseSeed()) {
            System.out.println("[" + getTimestamp() + "] Merchandise catalog already populated — skipping seed.");
            return;
        }

        System.out.println("[" + getTimestamp() + "] Seeding merchandise catalog with official product lineup...");

        List<String> lines = new ArrayList<>();
        lines.add("productId,name,description,category,basePrice,size,color,stockQuantity,active,imageUrl");

        // Apparel (KIT)
        addMerchandiseLine(lines, "PROD_001", "Official Home Jersey 2026",
                "Official 2026 Home Jersey — breathable moisture-wicking fabric, embroidered crest.",
                "KIT", 85.00, "S,M,L,XL,XXL", "Red/White", 150, true);
        addMerchandiseLine(lines, "PROD_002", "Official Away Jersey 2026",
                "Official 2026 Away Jersey — lightweight performance fabric, sublimated design.",
                "KIT", 85.00, "S,M,L,XL,XXL", "Navy/Gold", 120, true);
        addMerchandiseLine(lines, "PROD_003", "Pro Training Tracksuit",
                "Official Training Tracksuit — zip-front jacket with matching joggers, slim fit.",
                "KIT", 110.00, "S,M,L,XL,XXL", "Black/Gold", 75, true);

        // Footwear
        addMerchandiseLine(lines, "PROD_004", "Elite Football Boots (Spikes)",
                "Professional-grade cleats — lightweight upper, firm-ground studs, anatomical fit.",
                "FOOTWEAR", 145.00, "EU39,EU40,EU41,EU42,EU43,EU44,EU45", "Black/Red", 45, true);
        addMerchandiseLine(lines, "PROD_005", "Classic Lifestyle Sneakers",
                "Heritage-inspired off-pitch sneakers — leather upper, rubber cupsole, cushioned insole.",
                "FOOTWEAR", 95.00, "EU39,EU40,EU41,EU42,EU43,EU44,EU45", "White/Navy", 60, true);

        // Accessories
        addMerchandiseLine(lines, "PROD_006", "Matchday Grip Socks",
                "Official matchday socks — arch support, reinforced heel/toe, anti-slip grip.",
                "ACCESSORIES", 15.00, "S,M,L,XL", "Red", 400, true);
        addMerchandiseLine(lines, "PROD_007", "Club Premium Snapback Cap",
                "Adjustable snapback cap — embroidered club badge, curved peak, six-panel construction.",
                "ACCESSORIES", 30.00, "ONE_SIZE", "Black/White", 200, true);

        writeLines(dataDir.resolve("merchandise.csv"), lines);
        recordsGenerated += 7;
        System.out.println("[" + getTimestamp() + "] Merchandise catalog seeded: 7 products written.");
    }

    /**
     * Formats and appends a single merchandise CSV row.
     */
    private void addMerchandiseLine(List<String> lines, String id, String name,
                                     String desc, String cat, double price,
                                     String size, String color, int stock, boolean active) {
        lines.add(String.join(",",
                id,
                "\"" + name + "\"",
                "\"" + desc + "\"",
                cat,
                String.format("%.2f", price),
                size,
                color,
                String.valueOf(stock),
                String.valueOf(active),
                "/img/" + id + ".jpg"
        ));
    }

    /**
     * Scans all 16 CSV files and regenerates only the merchandise catalog
     * if it is missing or empty. Safe to call at any time.
     *
     * @return number of merchandise records seeded (0 if already present)
     */
    public int seedMerchandiseIfEmpty() {
        try {
            int before = recordsGenerated;
            doSeedMerchandiseIfEmpty();
            return recordsGenerated - before;
        } catch (IOException e) {
            System.err.println("[" + getTimestamp() + "] seedMerchandiseIfEmpty failed: " + e.getMessage());
            return 0;
        }
    }
    public int generateAll() {
        recordsGenerated = 0;
        System.out.println("\n[" + getTimestamp() + "] DATA GENERATOR: Starting population of " + dataDir);

        try {
            Files.createDirectories(dataDir);

            generateAccounts();
            generatePlayers();
            generateStaff();
            generateFans();
            generateStadiumSeats();
            generateMatches();
            generateTickets();
            generateTransactions();
            generateMerchandise();
            generateCartItems();
            generateSalaries();
            generateLeagueTable();
            generateTeamLineups();
            generateAttendanceRecords();
            generateLoyaltyPoints();
            generateAuditLog();
            doSeedMerchandiseIfEmpty();

            System.out.println("[" + getTimestamp() + "] DATA GENERATOR: Complete! Total records generated: " + recordsGenerated);

        } catch (IOException e) {
            System.err.println("DATA GENERATOR ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return recordsGenerated;
    }

    // ============ GENERATORS ============

    private void generateAccounts() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("username,passwordHash,role,status,lastLogin,failedLoginAttempts,createdAt,personId");

        lines.add("admin," + sha256("admin123") + ",ADMIN,ACTIVE,," + getTimestamp() + ",admin_id");
        lines.add("manager," + sha256("manager123") + ",QUAN_LY_QUAY,ACTIVE,," + getTimestamp() + ",manager_id");
        lines.add("coach," + sha256("coach123") + ",HLV_TRUONG,ACTIVE,," + getTimestamp() + ",coach_id");
        lines.add("hr_director," + sha256("hr123") + ",GIAM_DOC_NHAN_SU,ACTIVE,," + getTimestamp() + ",hr_id");
        lines.add("finance_director," + sha256("finance123") + ",GIAM_DOC_TAI_CHINH,ACTIVE,," + getTimestamp() + ",finance_id");
        lines.add("commentator," + sha256("commentator123") + ",TRONG_TAI,ACTIVE,," + getTimestamp() + ",commentator_id");
        recordsGenerated += 6;

        writeLines(dataDir.resolve("accounts.csv"), lines);
    }

    private String sha256(String password) {
        byte[] salt = new byte[16];
        rand.nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            String hashB64 = Base64.getEncoder().encodeToString(hashed);
            return saltB64 + ":" + hashB64;
        } catch (NoSuchAlgorithmException e) {
            return "ERR";
        }
    }

    private void generatePlayers() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("id,name,email,position,salary,fitness,injuryStatus,squadNumber,matchesPlayed,goalsScored,assists,joinDate,contractExpiry");

        Position[] positions = Position.values();
        String[] joinDates = {"2020-01-15", "2021-06-01", "2022-03-10", "2023-07-20", "2024-01-05"};
        String[] expiryDates = {"2026-06-30", "2027-06-30", "2028-06-30", "2029-06-30"};

        for (int i = 1; i <= 120; i++) {
            String id = "PLAYER_" + String.format("%04d", i);
            String name = generateName();
            String email = "player" + i + "@team.com";
            Position pos = positions[i % positions.length];
            double salary = 5000 + rand.nextDouble() * 95000;
            int fitness = 50 + rand.nextInt(51);
            InjuryStatus inj = rand.nextDouble() < 0.15 ? InjuryStatus.values()[rand.nextInt(5)] : InjuryStatus.HEALTHY;
            int squadNum = (i % 99) + 1;
            int matches = rand.nextInt(50);
            int goals = rand.nextInt(20);
            int assists = rand.nextInt(15);

            lines.add(String.join(",", id, name, email, pos.name(),
                String.format("%.2f", salary), String.valueOf(fitness), inj.name(),
                String.valueOf(squadNum), String.valueOf(matches),
                String.valueOf(goals), String.valueOf(assists),
                joinDates[i % joinDates.length], expiryDates[i % expiryDates.length]));
        }

        recordsGenerated += 120;
        writeLines(dataDir.resolve("players.csv"), lines);
    }

    private void generateStaff() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("id,name,email,specificRole,salary,department,startDate,endDate,active,qualifications,reportsTo");

        Staff.SpecificRole[] roles = Staff.SpecificRole.values();
        String[] departments = {"COACHING", "MEDICAL", "MARKETING", "OPERATIONS", "ADMINISTRATION"};
        String[] qualifications = {"UEFA A", "UEFA B", "BSc Sports Science", "MBBS", "MBA", "FA Level 2"};

        for (int i = 1; i <= 60; i++) {
            String id = "STAFF_" + String.format("%04d", i);
            String name = generateName();
            String email = "staff" + i + "@club.com";
            Staff.SpecificRole role = roles[i % roles.length];
            double salary = 3000 + rand.nextDouble() * 47000;
            String dept = departments[i % departments.length];
            String qual = qualifications[i % qualifications.length];

            lines.add(String.join(",", id, name, email, role.name(),
                String.format("%.2f", salary), dept, "2020-01-01", "", "true", qual, ""));
        }

        recordsGenerated += 60;
        writeLines(dataDir.resolve("staff.csv"), lines);
    }

    private void generateFans() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("id,name,email,accountBalance,loyaltyPoints,tier,phoneNumber,address,registeredDate,preferredSector,totalTicketsPurchased,totalSpend,marketingOptIn");

        for (int i = 1; i <= 500; i++) {
            String id = "FAN_" + String.format("%05d", i);
            String name = generateName();
            String email = "fan" + i + "@email.com";
            double balance = 100 + rand.nextDouble() * 9900;
            int points = rand.nextInt(60000);
            LoyaltyTier tier = LoyaltyTier.fromPoints(points);
            String phone = "+84-9" + String.format("%07d", rand.nextInt(10000000));
            String sector = STADIUM_SECTORS[rand.nextInt(STADIUM_SECTORS.length)];
            int tickets = rand.nextInt(100);
            double spend = tickets * (30 + rand.nextDouble() * 170);

            lines.add(String.join(",", id, name, email,
                String.format("%.2f", balance), String.valueOf(points), tier.name(),
                phone, "Address " + i, getRandomDate(), sector,
                String.valueOf(tickets), String.format("%.2f", spend), "true"));
        }

        recordsGenerated += 500;
        writeLines(dataDir.resolve("fans.csv"), lines);
    }

    private void generateStadiumSeats() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("seatId,sector,rowNum,seatNumber,price,status,version,hasGoodView,isCovered,description");

        int totalSeats = 0;
        int[] sectorSeatCounts = {200, 1500, 1500, 1000, 1000, 800, 200};
        double[] sectorPrices = {500, 150, 120, 80, 60, 40, 30};

        for (int s = 0; s < STADIUM_SECTORS.length; s++) {
            String sector = STADIUM_SECTORS[s];
            int count = sectorSeatCounts[s];
            double price = sectorPrices[s];
            int rows = Math.max(1, count / 50);

            for (int row = 0; row < rows; row++) {
                String rowLetter = String.valueOf((char) ('A' + (row % 26)));
                int seatsInRow = count / rows;

                for (int num = 1; num <= seatsInRow; num++) {
                    String seatId = sector + "-" + rowLetter + "-" + String.format("%03d", num);
                    SeatStatus status = rand.nextDouble() < 0.7 ? SeatStatus.AVAILABLE : SeatStatus.values()[rand.nextInt(SeatStatus.values().length)];
                    boolean goodView = rand.nextDouble() < 0.85;
                    boolean covered = sector.equals("VIP") || sector.equals("A");
                    String desc = goodView ? "Good view" : "Limited view";

                    lines.add(String.join(",", seatId, sector, rowLetter,
                        String.valueOf(num), String.format("%.2f", price), status.name(),
                        "1", String.valueOf(goodView), String.valueOf(covered), desc));
                    totalSeats++;
                }
            }
        }

        recordsGenerated += totalSeats;
        writeLines(dataDir.resolve("stadium_seats.csv"), lines);
    }

    private void generateMatches() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("matchId,homeTeam,awayTeam,venue,matchDate,matchTime,status,homeScore,awayScore,attendance,ticketPriceStandard,ticketPriceVip,competition,round,referee");

        LocalDate startDate = LocalDate.of(2026, 5, 1);
        MatchStatus[] statuses = {MatchStatus.COMPLETED, MatchStatus.COMPLETED,
            MatchStatus.SCHEDULED, MatchStatus.TICKETS_ON_SALE};

        for (int i = 1; i <= 50; i++) {
            String id = "MATCH_" + String.format("%03d", i);
            String home = TEAM_NAMES[rand.nextInt(TEAM_NAMES.length)];
            String away = TEAM_NAMES[rand.nextInt(TEAM_NAMES.length)];
            LocalDate date = startDate.plusDays(rand.nextInt(365));
            String time = String.format("%02d:%02d", 15 + rand.nextInt(4), rand.nextInt(4) * 15);
            MatchStatus status = statuses[i % statuses.length];
            int homeScore = status == MatchStatus.COMPLETED ? rand.nextInt(5) : 0;
            int awayScore = status == MatchStatus.COMPLETED ? rand.nextInt(5) : 0;
            int attendance = status == MatchStatus.COMPLETED ? 10000 + rand.nextInt(35000) : 0;
            String comp = COMPETITIONS[i % COMPETITIONS.length];

            lines.add(String.join(",", id, home, away, "Stadium",
                date.toString(), time, status.name(),
                String.valueOf(homeScore), String.valueOf(awayScore),
                String.valueOf(attendance), "50.00", "200.00", comp,
                String.valueOf((i % 38) + 1), "Referee " + (i % 20 + 1)));
        }

        recordsGenerated += 50;
        writeLines(dataDir.resolve("matches.csv"), lines);
    }

    private void generateTickets() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("ticketId,matchId,fanId,seatId,category,price,purchaseDate,matchDate,checkedIn,checkedInTime,transactionId");

        for (int i = 1; i <= 1500; i++) {
            String id = "TICKET_" + String.format("%06d", i);
            String matchId = "MATCH_" + String.format("%03d", (i % 50) + 1);
            String fanId = "FAN_" + String.format("%05d", (i % 500) + 1);
            String seatSector = STADIUM_SECTORS[rand.nextInt(STADIUM_SECTORS.length)];
            String seatId = seatSector + "-" + (char)('A' + rand.nextInt(10)) + "-" + String.format("%03d", rand.nextInt(100) + 1);
            TicketCategory cat = TicketCategory.values()[rand.nextInt(TicketCategory.values().length)];
            double price = 30 + rand.nextDouble() * 170;
            String purchaseDate = getRandomDate();
            String matchDate = "2026-" + String.format("%02d", (i % 12) + 1) + "-" + String.format("%02d", (i % 28) + 1);
            boolean checkedIn = rand.nextDouble() < 0.6;

            lines.add(String.join(",", id, matchId, fanId, seatId, cat.name(),
                String.format("%.2f", price), purchaseDate, matchDate,
                String.valueOf(checkedIn), checkedIn ? purchaseDate : "", "TXN_" + i));
        }

        recordsGenerated += 1500;
        writeLines(dataDir.resolve("tickets.csv"), lines);
    }

    private void generateTransactions() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("transactionId,timestamp,type,amount,description,referenceId,category,processedBy,reconciled");

        TransactionType[] incomeTypes = {TransactionType.TICKET_PURCHASE, TransactionType.MERCHANDISE_SALE,
            TransactionType.SPONSORSHIP_INCOME, TransactionType.BROADCASTING_REVENUE, TransactionType.PRIZE_MONEY};
        TransactionType[] expenseTypes = {TransactionType.SALARY_PAYMENT, TransactionType.MERCHANDISE_PURCHASE,
            TransactionType.MARKETING_EXPENSE, TransactionType.UTILITY_EXPENSE, TransactionType.INSURANCE_EXPENSE};

        for (int i = 1; i <= 600; i++) {
            String id = "TXN_" + String.format("%06d", i);
            String timestamp = getRandomTimestamp();
            boolean isIncome = rand.nextDouble() < 0.55;
            TransactionType type = isIncome ? incomeTypes[rand.nextInt(incomeTypes.length)]
                                           : expenseTypes[rand.nextInt(expenseTypes.length)];
            double amount = isIncome ? 1000 + rand.nextDouble() * 99000
                                     : 500 + rand.nextDouble() * 50000;
            String desc = type.getDescription();
            String refId = "REF_" + i;
            String cat = isIncome ? "REVENUE" : "EXPENSE";
            String processedBy = "admin";
            boolean reconciled = rand.nextDouble() < 0.7;

            lines.add(String.join(",", id, timestamp, type.name(),
                String.format("%.2f", amount), desc, refId, cat, processedBy,
                String.valueOf(reconciled)));
        }

        recordsGenerated += 600;
        writeLines(dataDir.resolve("transactions.csv"), lines);
    }

    private void generateMerchandise() throws IOException {
        Path merchPath = dataDir.resolve("merchandise.csv");
        // Skip if already populated — seedMerchandiseIfEmpty() or prior run wrote data.
        if (Files.exists(merchPath)) {
            try {
                long size = Files.size(merchPath);
                if (size > 0 && Files.readAllLines(merchPath, StandardCharsets.UTF_8).size() > 1) {
                    System.out.println("[" + getTimestamp() + "] merchandise.csv already has data — generateMerchandise() skipped.");
                    return;
                }
            } catch (IOException e) {
                // Fall through — attempt to write fresh.
            }
        }

        List<String> lines = new ArrayList<>();
        lines.add("productId,name,description,category,basePrice,size,color,stockQuantity,active,imageUrl");

        ProductCategory[] categories = ProductCategory.values();
        String[] sizes = {"S", "M", "L", "XL", "ONE_SIZE"};
        String[] colors = {"Red", "Blue", "White", "Black", "Green"};

        for (int i = 1; i <= 120; i++) {
            String id = "PROD_" + String.format("%04d", i);
            String name = PRODUCT_NAMES[i % PRODUCT_NAMES.length] + " v" + (i % 5 + 1);
            String desc = "Official " + name + " - Premium quality";
            ProductCategory cat = categories[i % categories.length];
            double price = 15 + rand.nextDouble() * 285;
            String size = sizes[rand.nextInt(sizes.length)];
            String color = colors[rand.nextInt(colors.length)];
            int stock = 10 + rand.nextInt(490);
            boolean active = rand.nextDouble() < 0.9;

            lines.add(String.join(",", id, name, desc, cat.name(),
                String.format("%.2f", price), size, color,
                String.valueOf(stock), String.valueOf(active), "/img/" + id + ".jpg"));
        }

        recordsGenerated += 120;
        writeLines(dataDir.resolve("merchandise.csv"), lines);
    }

    private void generateCartItems() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("cartItemId,fanId,productId,productName,quantity,size,color,unitPrice,addedAt");

        for (int i = 1; i <= 200; i++) {
            String id = "CART_" + String.format("%05d", i);
            String fanId = "FAN_" + String.format("%05d", (i % 500) + 1);
            String prodId = "PROD_" + String.format("%04d", (i % 120) + 1);
            String prodName = PRODUCT_NAMES[(i % PRODUCT_NAMES.length)];
            int qty = 1 + rand.nextInt(3);
            String size = "M";
            String color = "Blue";
            double price = 30 + rand.nextDouble() * 100;

            lines.add(String.join(",", id, fanId, prodId, prodName,
                String.valueOf(qty), size, color,
                String.format("%.2f", price), getRandomTimestamp()));
        }

        recordsGenerated += 200;
        writeLines(dataDir.resolve("cart_items.csv"), lines);
    }

    private void generateSalaries() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("recordId,personId,personType,grossSalary,deductions,netSalary,payPeriod,paymentDate,processedBy,status");

        for (int i = 1; i <= 300; i++) {
            String id = "SAL_" + String.format("%05d", i);
            boolean isPlayer = rand.nextDouble() < 0.65;
            String personId = isPlayer ? "PLAYER_" + String.format("%04d", (i % 120) + 1)
                                       : "STAFF_" + String.format("%04d", (i % 60) + 1);
            String personType = isPlayer ? "PLAYER" : "STAFF";
            double gross = 5000 + rand.nextDouble() * 95000;
            double deductions = gross * 0.1;
            double net = gross - deductions;
            String period = "2025-" + String.format("%02d", (i % 12) + 1);
            String date = period + "-25";
            double processedBy = 1;

            lines.add(String.join(",", id, personId, personType,
                String.format("%.2f", gross), String.format("%.2f", deductions),
                String.format("%.2f", net), period, date, "admin", "PAID"));
        }

        recordsGenerated += 300;
        writeLines(dataDir.resolve("salaries.csv"), lines);
    }

    private void generateLeagueTable() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("teamName,played,won,drawn,lost,goalsFor,goalsAgainst,goalDifference,points,season,position");

        String season = "2025-2026";

        for (int i = 0; i < TEAM_NAMES.length; i++) {
            String team = TEAM_NAMES[i];
            int played = 30 + rand.nextInt(10);
            int won = rand.nextInt(played);
            int drawn = rand.nextInt(played - won);
            int lost = played - won - drawn;
            int gf = 20 + rand.nextInt(80);
            int ga = 20 + rand.nextInt(60);
            int gd = gf - ga;
            int pts = won * 3 + drawn;

            lines.add(String.join(",", team, String.valueOf(played),
                String.valueOf(won), String.valueOf(drawn), String.valueOf(lost),
                String.valueOf(gf), String.valueOf(ga), String.valueOf(gd),
                String.valueOf(pts), season, String.valueOf(i + 1)));
        }

        recordsGenerated += TEAM_NAMES.length;
        writeLines(dataDir.resolve("league_table.csv"), lines);
    }

    private void generateTeamLineups() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("lineupId,matchId,teamName,formation,startingPlayers,substitutes,tactics,createdAt");

        for (int i = 1; i <= 30; i++) {
            String id = "LINEUP_" + String.format("%04d", i);
            String matchId = "MATCH_" + String.format("%03d", (i % 50) + 1);
            String team = TEAM_NAMES[rand.nextInt(TEAM_NAMES.length)];
            String formation = "4-3-3";
            String players = generatePlayerIds(11);
            String subs = generatePlayerIds(7);

            lines.add(String.join(",", id, matchId, team, formation, players, subs, "ATTACK", getRandomTimestamp()));
        }

        recordsGenerated += 30;
        writeLines(dataDir.resolve("team_lineups.csv"), lines);
    }

    private void generateAttendanceRecords() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("recordId,matchId,fanId,seatId,entryTime,exitTime,earlyArrival");

        for (int i = 1; i <= 500; i++) {
            String id = "ATT_" + String.format("%06d", i);
            String matchId = "MATCH_" + String.format("%03d", (i % 50) + 1);
            String fanId = "FAN_" + String.format("%05d", (i % 500) + 1);
            String seatSector = STADIUM_SECTORS[rand.nextInt(STADIUM_SECTORS.length)];
            String seatId = seatSector + "-" + (char)('A' + rand.nextInt(10)) + "-" + String.format("%03d", rand.nextInt(100) + 1);
            String entryTime = "2026-05-" + String.format("%02d", (i % 28) + 1) + " 14:" + String.format("%02d", rand.nextInt(60));
            boolean early = rand.nextDouble() < 0.3;

            lines.add(String.join(",", id, matchId, fanId, seatId, entryTime, "", String.valueOf(early)));
        }

        recordsGenerated += 500;
        writeLines(dataDir.resolve("attendance_records.csv"), lines);
    }

    private void generateLoyaltyPoints() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("recordId,fanId,points,transactionType,referenceId,timestamp,description");

        for (int i = 1; i <= 400; i++) {
            String id = "LP_" + String.format("%06d", i);
            String fanId = "FAN_" + String.format("%05d", (i % 500) + 1);
            int points = 10 + rand.nextInt(990);
            String type = rand.nextDouble() < 0.8 ? "EARN" : "REDEEM";
            String refId = "REF_" + i;
            String timestamp = getRandomTimestamp();
            String desc = type.equals("EARN") ? "Purchase reward points" : "Points redeemed for discount";

            lines.add(String.join(",", id, fanId, String.valueOf(points), type, refId, timestamp, desc));
        }

        recordsGenerated += 400;
        writeLines(dataDir.resolve("loyalty_points.csv"), lines);
    }

    private void generateAuditLog() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("logId,timestamp,username,action,resource,resourceId,ipAddress,result,details");

        String[] actions = {"LOGIN", "LOGOUT", "TICKET_BOOK", "TICKET_CANCEL", "MERCHANDISE_PURCHASE",
            "PROFILE_UPDATE", "SALARY_VIEW", "MATCH_UPDATE"};
        String[] users = {"admin", "manager", "coach", "hr_director", "finance_director"};
        String[] results = {"SUCCESS", "SUCCESS", "SUCCESS", "FAILED", "SUCCESS"};

        for (int i = 1; i <= 300; i++) {
            String id = "LOG_" + String.format("%06d", i);
            String timestamp = getRandomTimestamp();
            String user = users[rand.nextInt(users.length)];
            String action = actions[rand.nextInt(actions.length)];
            String resource = "TICKET";
            String refId = "REF_" + i;
            String ip = "192.168.1." + (rand.nextInt(255) + 1);
            String result = results[rand.nextInt(results.length)];
            String details = action + " by " + user;

            lines.add(String.join(",", id, timestamp, user, action, resource, refId, ip, result, details));
        }

        recordsGenerated += 300;
        writeLines(dataDir.resolve("audit_log.csv"), lines);
    }

    // ============ HELPERS ============

    private String generateName() {
        String first = FIRST_NAMES[rand.nextInt(FIRST_NAMES.length)];
        String last = LAST_NAMES[rand.nextInt(LAST_NAMES.length)];
        int suffix = rand.nextBoolean() ? (1 + rand.nextInt(99)) : 0;
        String mid = rand.nextBoolean() ? " " + FIRST_NAMES[rand.nextInt(FIRST_NAMES.length)] : "";
        return first + mid + " " + last + (suffix > 0 ? " " + suffix : "");
    }

    private String generatePlayerIds(int count) {
        StringBuilder sb = new StringBuilder();
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < count; i++) {
            int num;
            do { num = 1 + rand.nextInt(120); } while (used.contains(num));
            used.add(num);
            if (sb.length() > 0) sb.append(";");
            sb.append("PLAYER_").append(String.format("%04d", num));
        }
        return sb.toString();
    }

    private String getRandomDate() {
        int year = 2024 + rand.nextInt(3);
        int month = 1 + rand.nextInt(12);
        int day = 1 + rand.nextInt(28);
        return String.format("%d-%02d-%02d", year, month, day);
    }

    private String getRandomTimestamp() {
        return getRandomDate() + " " + String.format("%02d:%02d:%02d",
            rand.nextInt(24), rand.nextInt(60), rand.nextInt(60));
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private void writeLines(Path file, List<String> lines) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        try {
            WriteAheadLog.writeWithWal(file, sb.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("WAL write interrupted", e);
        }
    }

    public int getRecordsGenerated() {
        return recordsGenerated;
    }
}
