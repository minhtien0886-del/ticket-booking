package com.club.ui;

import com.club.model.*;
import com.club.security.*;
import com.club.controller.*;
import com.club.service.*;
import com.club.service.TicketService;
import com.club.simulator.*;
import com.club.repository.*;
import com.club.exception.*;
import com.club.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Adaptable multi-level Secure CLI Menu system for the FCM-ERP.
 * Provides role-based menu navigation with interactive command input.
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public class SecureMenu {

    private final AuthenticationManager authManager;
    private UserSession currentSession;
    private boolean running = true;

    // Repositories
    private AccountRepository accountRepo;
    private FanRepository fanRepo;
    private PlayerRepository playerRepo;
    private StaffRepository staffRepo;
    private SeatRepository seatRepo;
    private TicketRepository ticketRepo;
    private MatchRepository matchRepo;
    private TransactionRepository transactionRepo;
    private MerchandiseRepository merchandiseRepo;
    private CartItemRepository cartItemRepo;
    private SalaryRepository salaryRepo;
    private LeagueTableRepository leagueTableRepo;
    private TeamLineupRepository teamLineupRepo;
    private AttendanceRepository attendanceRepo;
    private LoyaltyPointsRepository loyaltyPointsRepo;
    private AuditLogRepository auditLogRepo;

    // Services
    private FinanceService financeService;
    private InventoryService inventoryService;
    private ShoppingCartService shoppingCartService;
    private TicketService ticketService;
    private HumanResourceService hrService;

    // Controllers
    private FanController fanController;
    private StadiumController stadiumController;
    private TicketController ticketController;
    private StaffController staffController;

    // Simulator
    private TicketSimulator simulator;

    private final Scanner scanner;

    public SecureMenu(Path dataDir) {
        this.authManager = AuthenticationManager.getInstance();
        this.scanner = new Scanner(System.in);
        initializeRepositories(dataDir);
        initializeServices();
        initializeControllers();
    }

    private void initializeRepositories(Path dataDir) {
        accountRepo = new AccountRepository(dataDir);
        fanRepo = new FanRepository(dataDir);
        playerRepo = new PlayerRepository(dataDir);
        staffRepo = new StaffRepository(dataDir);
        seatRepo = SeatRepository.getInstance();
        ticketRepo = new TicketRepository(dataDir);
        matchRepo = new MatchRepository(dataDir);
        transactionRepo = new TransactionRepository(dataDir);
        merchandiseRepo = new MerchandiseRepository(dataDir);
        cartItemRepo = new CartItemRepository(dataDir);
        salaryRepo = new SalaryRepository(dataDir);
        leagueTableRepo = new LeagueTableRepository(dataDir);
        teamLineupRepo = new TeamLineupRepository(dataDir);
        attendanceRepo = new AttendanceRepository(dataDir);
        loyaltyPointsRepo = new LoyaltyPointsRepository(dataDir);
        auditLogRepo = new AuditLogRepository(dataDir);

        try {
            accountRepo.ensureLoaded();
            fanRepo.ensureLoaded();
            playerRepo.ensureLoaded();
            staffRepo.ensureLoaded();
            seatRepo.ensureLoaded();
            ticketRepo.ensureLoaded();
            matchRepo.ensureLoaded();
            transactionRepo.ensureLoaded();
            merchandiseRepo.ensureLoaded();
            cartItemRepo.ensureLoaded();
            salaryRepo.ensureLoaded();
            leagueTableRepo.ensureLoaded();
            teamLineupRepo.ensureLoaded();
            attendanceRepo.ensureLoaded();
            loyaltyPointsRepo.ensureLoaded();
            auditLogRepo.ensureLoaded();
        } catch (IOException e) {
            System.err.println("Warning: Failed to load some repositories: " + e.getMessage());
        }

        authManager.setAccountRepository(accountRepo);
        authManager.setFanRepository(fanRepo);
        authManager.setStaffRepository(staffRepo);
        authManager.setPlayerRepository(playerRepo);
    }

    private void initializeServices() {
        financeService = new FinanceService(transactionRepo, salaryRepo, accountRepo);
        inventoryService = new InventoryService(merchandiseRepo);
        shoppingCartService = new ShoppingCartService(cartItemRepo, merchandiseRepo, fanRepo,
            financeService, inventoryService);
        ticketService = new TicketService(ticketRepo, seatRepo, matchRepo, fanRepo,
            transactionRepo, financeService);
        hrService = new HumanResourceService(playerRepo, staffRepo, salaryRepo);
    }

    private void initializeControllers() {
        fanController = new FanController(accountRepo, fanRepo, ticketRepo);
        stadiumController = new StadiumController(seatRepo);
        ticketController = new TicketController(ticketService, matchRepo, seatRepo);
        staffController = new StaffController(hrService, playerRepo, staffRepo);

        simulator = new TicketSimulator(ticketService, seatRepo, ticketRepo,
            matchRepo, fanRepo, transactionRepo);
    }

    // ============ MAIN ENTRY ============

    public void start() {
        printBanner();

        while (running) {
            if (currentSession == null) {
                showLoginMenu();
            } else {
                showMainMenu();
            }
        }

        System.out.println("\nThank you for using FCM-ERP. Goodbye!");
    }

    public void stop() {
        running = false;
    }

    // ============ LOGIN / REGISTRATION ============

    private void showLoginMenu() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("              FCM-ERP - FOOTBALL CLUB MANAGEMENT SYSTEM");
        System.out.println(repeat("=", 60));
        System.out.println("  [1] Login with Username & Password");
        System.out.println("  [2] Register as New Fan");
        System.out.println("  [3] View Match Schedule (Public)");
        System.out.println("  [4] View Stadium Seat Map (Public)");
        System.out.println("  [5] Exit");
        System.out.println(repeat("=", 60));
        System.out.print("  Select option: ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1": doLogin(); break;
            case "2": doRegister(); break;
            case "3": viewMatchSchedulePublic(); break;
            case "4": viewStadiumMapPublic(); break;
            case "5": stop(); break;
            default: System.out.println("  Invalid option.");
        }
    }

    private void doLogin() {
        System.out.print("  Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("  Password: ");
        String password = scanner.nextLine();

        try {
            currentSession = authManager.login(username, password, "127.0.0.1");
            System.out.println("\n  Welcome, " + currentSession.getPersonName() + "!");
            System.out.println("  Role: " + currentSession.getRole().getDescription());
            System.out.println("  Session ID: " + currentSession.getSessionId().substring(0, 8) + "...");
        } catch (AuthenticationException e) {
            System.out.println("\n  LOGIN FAILED: " + e.getMessage());
        }
    }

    private void doRegister() {
        System.out.println("\n  --- FAN REGISTRATION ---");
        System.out.print("  Full Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("  Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("  Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("  Password (min 6 chars): ");
        String password = scanner.nextLine();
        System.out.print("  Initial Deposit (0 for none): ");
        double deposit = 0;
        try { deposit = Double.parseDouble(scanner.nextLine().trim()); } catch (Exception e) {}

        try {
            Fan fan = fanController.registerFan(name, email, username, password, deposit);
            System.out.println("\n  REGISTRATION SUCCESSFUL!");
            System.out.println("  Fan ID: " + fan.getId());
            System.out.println("  Loyalty Tier: " + fan.getTier().getDisplayName());
        } catch (DuplicateEntityException e) {
            System.out.println("\n  REGISTRATION FAILED: " + e.getMessage());
        } catch (ValidationException e) {
            System.out.println("\n  VALIDATION ERROR: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("\n  REGISTRATION FAILED: " + e.getMessage());
        }
    }

    // ============ MAIN MENU ============

    private void showMainMenu() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("  FCM-ERP - MAIN DASHBOARD");
        System.out.println(repeat("-", 60));
        System.out.printf("  Logged in: %s | Role: %s%n",
            currentSession.getPersonName(), currentSession.getRole().getDescription());

        System.out.println("  [1] View Stadium Seat Map");
        System.out.println("  [2] View Match Schedule");
        System.out.println("  [3] Ticket Booking");
        System.out.println("  [4] Merchandise Shop");
        System.out.println("  [5] My Account & Loyalty");
        System.out.println("  [6] Shopping Cart");

        if (currentSession.hasPermission(Permission.VIEW_FINANCIAL_REPORTS)) {
            System.out.println("  [7] Financial Reports");
        }
        if (currentSession.hasPermission(Permission.MANAGE_HUMAN_RESOURCE)) {
            System.out.println("  [8] HR Management");
        }
        if (currentSession.hasPermission(Permission.UPDATE_LIVE_STATS)) {
            System.out.println("  [9] Match Statistics");
        }
        if (currentSession.hasPermission(Permission.RUN_SIMULATOR)) {
            System.out.println("  [S] Ticket Simulator");
        }
        if (currentSession.hasPermission(Permission.VIEW_SEAT_MAP)) {
            System.out.println("  [M] Stadium Management");
        }

        System.out.println("  [L] Logout");
        System.out.println("  [X] Exit");
        System.out.println(repeat("=", 60));
        System.out.print("  Select option: ");

        String choice = scanner.nextLine().trim().toUpperCase();

        switch (choice) {
            case "1": viewStadiumMap(); break;
            case "2": viewMatchSchedule(); break;
            case "3": ticketBookingMenu(); break;
            case "4": merchandiseShopMenu(); break;
            case "5": accountMenu(); break;
            case "6": shoppingCartMenu(); break;
            case "7": showFinancialReports(); break;
            case "8": showHRMenu(); break;
            case "9": showMatchStatsMenu(); break;
            case "S": showSimulatorMenu(); break;
            case "M": showStadiumManagementMenu(); break;
            case "L": doLogout(); break;
            case "X": stop(); break;
            default: System.out.println("  Invalid option.");
        }
    }

    // ============ SUB MENUS ============

    private void viewStadiumMap() {
        stadiumController.renderStadiumMap();
        pause();
    }

    private void viewStadiumMapPublic() {
        stadiumController.renderStadiumMap();
        pause();
    }

    private void viewMatchSchedule() {
        List<Match> matches = matchRepo.findUpcoming();
        System.out.println("\n" + repeat("=", 70));
        System.out.println("                      UPCOMING MATCHES");
        System.out.println(repeat("=", 70));
        System.out.printf("  %-12s %-20s %-15s %-10s %-10s%n",
            "Match ID", "Teams", "Date", "Time", "Status");
        System.out.println(repeat("-", 70));

        for (Match m : matches) {
            System.out.printf("  %-12s %-20s %-15s %-10s %-10s%n",
                m.getMatchId(),
                m.getHomeTeam() + " vs " + m.getAwayTeam(),
                m.getMatchDate(),
                m.getMatchTime(),
                m.getStatus().name());
        }

        System.out.println(repeat("=", 70));
        pause();
    }

    private void viewMatchSchedulePublic() {
        viewMatchSchedule();
    }

    private void ticketBookingMenu() {
        System.out.println("\n  --- TICKET BOOKING ---");
        List<Match> matches = matchRepo.findUpcoming();
        if (matches.isEmpty()) {
            System.out.println("  No matches available for booking.");
            return;
        }

        System.out.println("  Select a match (enter number):");
        for (int i = 0; i < matches.size(); i++) {
            Match m = matches.get(i);
            System.out.printf("  [%d] %s vs %s - %s %s%n",
                i + 1, m.getHomeTeam(), m.getAwayTeam(), m.getMatchDate(), m.getMatchTime());
        }

        System.out.print("  Choice: ");
        int choice = readInt() - 1;
        if (choice < 0 || choice >= matches.size()) {
            System.out.println("  Invalid selection.");
            return;
        }

        Match selected = matches.get(choice);
        System.out.println("\n  Selected: " + selected.getHomeTeam() + " vs " + selected.getAwayTeam());

        stadiumController.renderStadiumMap();

        System.out.println("\n  HOW TO ENTER SEATS:");
        System.out.println("    - Full ID:    VIP-A-012, A-B-015         (SECTOR-ROW-NUMBER)");
        System.out.println("    - Sector+Row: F-A-S01, VIP-C-S12        (SECTOR-ROW-POSITION)");
        System.out.println("    - Pos only:   A-S01, A-S02                 (sector + S## from map)");
        System.out.println("    - Mixed:     A-S01, A-B-015, VIP-A-S05   (comma-separated, max 4 seats)");
        System.out.println("    - Note: S## is the position number shown below each seat on the map.");
        System.out.print("\n  Enter seat IDs (comma-separated, max 4): ");
        String input = scanner.nextLine().trim();
        List<String> seatIds = resolvePositionInput(input, selected);

        if (seatIds.isEmpty()) {
            System.out.println("  No seats selected.");
            return;
        }

        try {
            List<Ticket> tickets = ticketService.bookTickets(
                currentSession.getPersonId(), selected.getMatchId(), seatIds, currentSession.getUsername());
            System.out.println("\n  BOOKING SUCCESSFUL! " + tickets.size() + " ticket(s) issued:");
            for (Ticket t : tickets) {
                System.out.printf("    Ticket ID: %s | Seat: %s | Price: %.2f%n",
                    t.getTicketId(), t.getSeatId(), t.getPrice());
            }
        } catch (ExceedsMaxTicketsException e) {
            System.out.println("  ERROR: " + e.getMessage());
        } catch (SeatNotAvailableException e) {
            System.out.println("  ERROR: Seat not available - " + e.getMessage());
        } catch (InsufficientBalanceException e) {
            System.out.println("  ERROR: Insufficient balance - " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }

        pause();
    }

    private void merchandiseShopMenu() {
        System.out.println("\n  --- MERCHANDISE SHOP ---");
        List<Merchandise> items = inventoryService.getInStockItems();

        System.out.printf("  %-12s %-30s %-15s %-8s %-8s%n",
            "Product ID", "Name", "Category", "Price", "Stock");
        System.out.println(repeat("-", 75));

        for (Merchandise m : items) {
            System.out.printf("  %-12s %-30s %-15s %-8.2f %-8d%n",
                m.getProductId(), m.getName().length() > 28 ? m.getName().substring(0, 28) : m.getName(),
                m.getCategory().name(), m.getBasePrice(), m.getStockQuantity());
        }

        System.out.println(repeat("-", 75));
        System.out.print("  Enter product ID to add to cart (or ENTER to go back): ");
        String input = scanner.nextLine().trim();

        if (!input.isEmpty()) {
            System.out.print("  Quantity (1-10): ");
            int qty = readInt();
            try {
                Merchandise m = merchandiseRepo.findById(input);
                if (m != null) {
                    shoppingCartService.addToCart(currentSession.getPersonId(), input, qty, m.getSize(), m.getColor());
                    System.out.println("  Added to cart: " + m.getName() + " x" + qty);
                } else {
                    System.out.println("  Product not found.");
                }
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getMessage());
            }
        }
    }

    private void accountMenu() {
        System.out.println("\n  --- MY ACCOUNT ---");
        Fan fan = fanRepo.findById(currentSession.getPersonId());
        if (fan == null) {
            System.out.println("  Account not found.");
            return;
        }

        System.out.printf("  Name:          %s%n", fan.getName());
        System.out.printf("  Email:         %s%n", fan.getEmail());
        System.out.printf("  Balance:       %.2f%n", fan.getAccountBalance());
        System.out.printf("  Loyalty Tier:  %s%n", fan.getTier().getDisplayName());
        System.out.printf("  Points:        %d%n", fan.getLoyaltyPoints());
        System.out.printf("  Total Spend:   %.2f%n", fan.getTotalSpend());
        System.out.printf("  Tickets Bought: %d%n", fan.getTotalTicketsPurchased());

        System.out.print("\n  Deposit funds? (y/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.print("  Amount: ");
            double amount = readDouble();
            try {
                fanController.deposit(fan.getId(), amount);
                System.out.println("  Deposit successful!");
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getMessage());
            }
        }
    }

    private void shoppingCartMenu() {
        System.out.println("\n  --- SHOPPING CART ---");
        List<CartItem> items = shoppingCartService.getCartItems(currentSession.getPersonId());

        if (items.isEmpty()) {
            System.out.println("  Your cart is empty.");
            return;
        }

        System.out.printf("  %-10s %-20s %-6s %-10s %-10s%n",
            "Item ID", "Product", "Qty", "Unit Price", "Subtotal");
        System.out.println(repeat("-", 60));

        for (CartItem ci : items) {
            System.out.printf("  %-10s %-20s %-6d %-10.2f %-10.2f%n",
                ci.getCartItemId().substring(0, 8),
                ci.getProductName().length() > 18 ? ci.getProductName().substring(0, 18) : ci.getProductName(),
                ci.getQuantity(), ci.getUnitPrice(), ci.getSubtotal());
        }

        double total = shoppingCartService.getCartTotal(currentSession.getPersonId());
        System.out.println(repeat("-", 60));
        System.out.printf("  TOTAL: %.2f%n", total);

        System.out.print("\n  [C] Checkout | [R] Remove item | [ENTER] Back: ");
        String choice = scanner.nextLine().trim().toUpperCase();

        if (choice.equals("C")) {
            try {
                Transaction t = shoppingCartService.checkout(currentSession.getPersonId(), currentSession.getUsername());
                System.out.println("  CHECKOUT SUCCESSFUL! Transaction: " + t.getTransactionId());
            } catch (InsufficientBalanceException e) {
                System.out.println("  ERROR: Insufficient balance.");
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getMessage());
            }
        } else if (choice.equals("R")) {
            System.out.print("  Enter item ID to remove: ");
            String id = scanner.nextLine().trim();
            try {
                shoppingCartService.removeFromCart(id);
                System.out.println("  Item removed.");
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getMessage());
            }
        }
    }

    private void showFinancialReports() {
        System.out.println("\n  --- FINANCIAL REPORTS ---");
        FinanceService.FinancialSummary summary = financeService.generateFinancialSummary();
        System.out.println(summary.toFormattedReport());
        pause();
    }

    private void showHRMenu() {
        System.out.println("\n  --- HR MANAGEMENT ---");
        HumanResourceService.PersonnelReport report = hrService.generatePersonnelReport();
        System.out.println(report.toFormattedReport());
        pause();
    }

    private void showMatchStatsMenu() {
        System.out.println("\n  --- MATCH STATISTICS ---");
        List<Match> completed = matchRepo.findCompleted();
        System.out.printf("  Total completed matches: %d%n", completed.size());

        for (int i = 0; i < Math.min(5, completed.size()); i++) {
            Match m = completed.get(i);
            System.out.printf("  %s %s %s %s - %s %n",
                m.getMatchId(), m.getHomeTeam(), m.getHomeScore(),
                m.getAwayScore(), m.getAwayTeam(), m.getStatus());
        }
        pause();
    }

    private void showStadiumManagementMenu() {
        System.out.println("\n  --- STADIUM MANAGEMENT ---");
        System.out.println("  [1] View Full Seat Map");
        System.out.println("  [2] View by Sector");
        System.out.println("  [3] View Sector Statistics");
        System.out.print("  Choice: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                stadiumController.renderStadiumMap();
                break;
            case "2":
                System.out.print("  Enter sector (VIP, A, B, C, D, E, F): ");
                String sector = scanner.nextLine().trim().toUpperCase();
                stadiumController.renderSectorMap(sector);
                break;
            case "3":
                Map<String, StadiumController.SectorStats> stats = stadiumController.getSectorStats();
                System.out.println("\n  SECTOR STATISTICS:");
                System.out.printf("  %-8s %-8s %-8s %-8s %-8s %-8s%n",
                    "Sector", "Total", "Available", "Booked", "Locked", "Occupancy%");
                System.out.println(repeat("-", 55));
                for (StadiumController.SectorStats s : stats.values()) {
                    System.out.printf("  %-8s %-8d %-8d %-8d %-8d %-8.1f%n",
                        s.sector, s.total, s.available, s.booked, s.locked, s.getOccupancyRate());
                }
                break;
        }
        pause();
    }

    private void showSimulatorMenu() {
        System.out.println("\n  --- TICKET SIMULATOR ---");
        System.out.println("  WARNING: This stress-tests the booking system with concurrent threads!");
        System.out.println();
        System.out.println("  [1] Run with SYNCHRONIZED lock (500 threads)");
        System.out.println("  [2] Run with FILE_LOCK (500 threads)");
        System.out.println("  [3] Run with OPTIMISTIC lock (500 threads)");
        System.out.println("  [4] Run with NO_LOCK (500 threads)");
        System.out.println("  [5] Run FULL COMPARATIVE benchmark (all modes)");
        System.out.println("  [C] Configure thread count");
        System.out.print("  Choice: ");

        String choice = scanner.nextLine().trim().toUpperCase();

        List<String> targetSeats = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            targetSeats.add("VIP-A-" + String.format("%03d", i + 1));
        }

        simulator.setTargetSeats(targetSeats);
        simulator.setMatchId("MATCH_001");

        int threads = 500;
        switch (choice) {
            case "1":
                simulator.setLockMode(TicketService.LockMode.SYNCHRONIZED);
                simulator.setThreadCount(threads);
                simulator.run();
                break;
            case "2":
                simulator.setLockMode(TicketService.LockMode.FILE_LOCK);
                simulator.setThreadCount(threads);
                simulator.run();
                break;
            case "3":
                simulator.setLockMode(TicketService.LockMode.OPTIMISTIC);
                simulator.setThreadCount(threads);
                simulator.run();
                break;
            case "4":
                simulator.setLockMode(TicketService.LockMode.NO_LOCK);
                simulator.setThreadCount(threads);
                simulator.run();
                break;
            case "5":
                simulator.runComparativeBenchmark(targetSeats, threads);
                break;
            case "C":
                System.out.print("  Thread count (100-1000): ");
                threads = readInt();
                simulator.setThreadCount(threads);
                System.out.println("  Thread count set to " + threads);
                break;
        }
        pause();
    }

    private void doLogout() {
        if (currentSession != null) {
            authManager.logout("User requested logout");
            currentSession = null;
            System.out.println("  You have been logged out.");
        }
    }

    // ============ UTILITIES ============

    private void pause() {
        System.out.print("\n  Press ENTER to continue...");
        scanner.nextLine();
    }

    private int readInt() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private double readDouble() {
        try {
            return Double.parseDouble(scanner.nextLine().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void printBanner() {
        System.out.println();
        System.out.println("  +++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("  +                                                     +");
        System.out.println("  +            FCM-ERP - FOOTBALL CLUB MANAGEMENT       +");
        System.out.println("  +            & HIGH-CONCURRENCY TICKETING ERP         +");
        System.out.println("  +                                                     +");
        System.out.println("  +          Group 4 - LAB211 - Phu, Phat, Tien         +");
        System.out.println("  +                                                     +");
        System.out.println("  +++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println();
    }

    /**
     * Resolves user input tokens into concrete seat ID strings for booking.
     *
     * <p>This method handles three token formats:</p>
     * <ol>
     *   <li><b>Full ID</b> (e.g. {@code VIP-A-012}): passed through unchanged —
     *       the caller is responsible for validating existence.</li>
     *   <li><b>Sector-Row-Position</b> (e.g. {@code F-A-S01}, {@code VIP-C-S12}):
     *       the token is parsed into its three components, the matching seats are
     *       queried from the cache, sorted by physical seat number, and the
     *       position-indexed seat is returned.</li>
     *   <li><b>Sector-Position</b> (e.g. {@code A-S01}): same as above but the
     *       row is implicitly determined by walking every row in the sector
     *       sequentially.</li>
     *   <li><b>Position-only</b> (e.g. {@code S01}): the seat is resolved by
     *       walking all available seats in the entire stadium.</li>
     * </ol>
     *
     * <p>Each resolved seat ID is validated against the cache — if the concrete ID
     * does not exist, an {@link EntityNotFoundException} is thrown with a helpful
     * message showing the original token and the derived ID.</p>
     *
     * @param input the raw comma-separated user input string
     * @param match the currently selected match (used only for the global-position case)
     * @return a list of concrete seat IDs (e.g. {@code ["VIP-A-012", "A-B-015"]})
     * @throws EntityNotFoundException if a position token resolves to a seat ID
     *         that does not exist in the repository cache
     */
    private List<String> resolvePositionInput(String input, Match match) {
        List<String> resolved = new ArrayList<>();

        for (String token : input.split(",")) {
            String tok = token.trim();
            if (tok.isEmpty()) continue;

            // ─────────────────────────────────────────────────────────────────────
            // FORMAT 1: Full ID — SECTOR-ROW-NUMBER (e.g. VIP-A-012)
            // Pass through unchanged; the ticket booking service validates existence.
            // ─────────────────────────────────────────────────────────────────────
            if (tok.matches("^[A-Z]+-[A-Z]-\\d{3,4}$")) {
                resolved.add(tok);
                continue;
            }

            // ─────────────────────────────────────────────────────────────────────
            // FORMAT 2: 3-part position — SECTOR-ROW-POSITION (e.g. F-A-S01, VIP-C-S12)
            // Pattern: [SECTOR]-[ROW]-[S##]
            // Example: "VIP-C-S12"
            //   parts[0] = "VIP"   → sector
            //   parts[1] = "C"     → row letter
            //   parts[2] = "S12"   → position number → index = 12 (1-based)
            // ─────────────────────────────────────────────────────────────────────
            if (tok.matches("^[A-Z_]+-[A-Z]-S\\d{1,3}$")) {
                // Split on the last "-S" boundary so that the sector may itself
                // contain underscores (e.g. SECTOR_A) without ambiguity.
                int dashS = tok.lastIndexOf("-S");
                // Extract sector+row by taking everything before "dashS".
                // e.g. "VIP-C-S12".lastIndexOf("-S") = 5 → "VIP-C" (indices 0..4)
                String sectorAndRow = tok.substring(0, dashS);
                // The position number string, e.g. "S12".
                String posStr = tok.substring(dashS + 1); // "+1" skips the '-' itself

                // sectorAndRow is "VIP-C" — split once on the first '-' to separate.
                // This guarantees we handle sector names like "VIP" or "SECTOR_A".
                int firstDash = sectorAndRow.indexOf('-');
                String sector = sectorAndRow.substring(0, firstDash);    // "VIP"
                String row    = sectorAndRow.substring(firstDash + 1);  // "C"

                // Strip the leading 'S' from "S12" and convert to 1-based integer.
                // "S12" → "12" → 12.  Subtracting 1 converts to 0-based index.
                int posIndex = Integer.parseInt(posStr.substring(1)) - 1;

                // Query the repository cache for all seats in this sector and row.
                // O(N) scan of the ConcurrentHashMap — acceptable since the map
                // is already in memory and this runs once per token per booking.
                List<Seat> rowSeats = new ArrayList<>(seatRepo.findBySectorAndRow(sector, row));
                if (rowSeats.isEmpty()) {
                    throw new EntityNotFoundException("Seat", sector + "-" + row
                            + ": no seats found for this sector/row combination");
                }

                // Sort seats by their physical seat number so that S01 maps to the
                // first seat in the row, S02 to the second, etc.
                rowSeats.sort(Comparator.comparingInt(Seat::getSeatNumber));

                // Bounds-check: position numbers shown on the map are 1-based.
                if (posIndex < 0 || posIndex >= rowSeats.size()) {
                    throw new EntityNotFoundException("Seat", sector + "-" + row
                            + ": position S" + (posIndex + 1)
                            + " is out of range for this row (max S"
                            + rowSeats.size() + ")");
                }

                Seat target = rowSeats.get(posIndex);
                resolved.add(target.getSeatId());
                continue;
            }

            // ─────────────────────────────────────────────────────────────────────
            // FORMAT 3: 2-part position — SECTOR-POSITION (e.g. A-S01)
            // Pattern: [SECTOR]-[S##]
            // The row is determined by walking rows in sorted order (A, B, C...).
            // Example: "A-S01" with SECTOR=A, posStr="S01", posIndex=0
            //   → Row A: seats [A-A-001, A-A-002, ...]   (posIndex=0 → A-A-001)
            //   → Row B: seats [A-B-001, A-B-002, ...]   (posIndex=2 → A-B-003)
            // ─────────────────────────────────────────────────────────────────────
            if (tok.matches("^[A-Z]+-S\\d{1,3}$")) {
                int dashS = tok.lastIndexOf("-S");
                String sector   = tok.substring(0, dashS);      // "A"
                String posStr   = tok.substring(dashS + 1);      // "S01"
                int posIndex    = Integer.parseInt(posStr.substring(1)) - 1;

                // Collect every seat in this sector, grouped by row letter.
                // LinkedHashMap preserves insertion order — important so that when
                // we iterate rows sequentially, we visit Row A before Row B, etc.
                Map<String, List<Seat>> byRow = new LinkedHashMap<>();
                for (Seat s : new ArrayList<>(seatRepo.findBySector(sector))) {
                    byRow.computeIfAbsent(s.getRowNum(), k -> new ArrayList<>()).add(s);
                }

                if (byRow.isEmpty()) {
                    throw new EntityNotFoundException("Seat", sector
                            + ": sector not found or has no seats");
                }

                int globalIdx = 0;
                outer:
                for (Map.Entry<String, List<Seat>> entry : byRow.entrySet()) {
                    List<Seat> sorted = entry.getValue();
                    sorted.sort(Comparator.comparingInt(Seat::getSeatNumber));
                    for (Seat s : sorted) {
                        if (globalIdx == posIndex) {
                            resolved.add(s.getSeatId());
                            break outer;
                        }
                        globalIdx++;
                    }
                }

                // If we exit the loop without matching, posIndex exceeded the
                // total seat count for this sector. Detect via totalSeats.
                int totalSeats = byRow.values().stream()
                        .mapToInt(List::size).sum();
                if (posIndex >= totalSeats) {
                    throw new EntityNotFoundException("Seat",
                            sector + ": position S" + (posIndex + 1)
                            + " is out of range (sector " + sector
                            + " has " + totalSeats + " seats)");
                }
                continue;
            }

            // ─────────────────────────────────────────────────────────────────────
            // FORMAT 4: Position-only — S## (e.g. S01) — global stadium position
            // Resolves across ALL sectors and rows in sorted order.
            // This is the least precise format — the stadium map position numbers
            // restart at S01 for each displayed row, so this format is ambiguous
            // unless the user only has one seat in mind.
            // ─────────────────────────────────────────────────────────────────────
            if (tok.matches("^S\\d{1,3}$")) {
                int posIndex = Integer.parseInt(tok.substring(1)) - 1;
                int globalIdx = 0;
                for (Seat seat : seatRepo.findAll()) {
                    if (globalIdx == posIndex) {
                        resolved.add(seat.getSeatId());
                        break;
                    }
                    globalIdx++;
                }
                continue;
            }

            // ─────────────────────────────────────────────────────────────────────
            // FALLBACK: Unknown format — treat as-is and let the booking service
            // handle the validation error.  This makes the system robust to future
            // format extensions without needing to modify this method.
            // ─────────────────────────────────────────────────────────────────────
            resolved.add(tok);
        }

        // Post-condition: max 4 seats per booking (enforced by caller).
        return resolved;
    }

    private String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }
}
