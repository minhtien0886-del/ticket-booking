package com.club;

import com.club.ui.*;
import com.club.util.*;
import com.club.repository.*;
import java.io.*;
import java.nio.file.*;

/**
 * Main entry point for the Football Club Management & Ticketing ERP System (FCM-ERP).
 *
 * <p>Initializes the system by:</p>
 * <ol>
 *   <li>Setting up the data directory and checking for data files</li>
 *   <li>Running the DataGenerator to populate CSV files with 10,000+ records on first run</li>
 *   <li>Loading all repository caches from CSV files</li>
 *   <li>Launching the interactive SecureMenu CLI</li>
 * </ol>
 *
 * <p>Default credentials for testing:</p>
 * <ul>
 *   <li>admin / admin123 — ADMIN role (full access)</li>
 *   <li>manager / manager123 — QUAN_LY_QUAY role</li>
 *   <li>coach / coach123 — HLV_TRUONG role</li>
 *   <li>hr_director / hr123 — GIAM_DOC_NHAN_SU role</li>
 *   <li>finance_director / finance123 — GIAM_DOC_TAI_CHINH role</li>
 *   <li>commentator / commentator123 — TRONG_TAI role</li>
 * </ul>
 *
 * <p>Note: Password hashes in the generated data are placeholders.
 * The AuthenticationManager hashes passwords at registration time.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 */
public final class ClubApplication {

    /** Application name. */
    public static final String APP_NAME = "FCM-ERP";

    /** Application version. */
    public static final String VERSION = "1.0.0";

    /** Default data directory name. */
    public static final String DATA_DIR_NAME = "data";

    /** Benchmark results file name. */
    public static final String BENCHMARK_FILE = "benchmark_results.csv";

    private final Path dataDir;
    private final Path benchmarkFile;

    public ClubApplication(Path dataDir) {
        this.dataDir = dataDir;
        this.benchmarkFile = dataDir.resolve(BENCHMARK_FILE);
    }

    /**
     * Initializes the application — generates data if needed, then starts the CLI menu.
     *
     * @param args command-line arguments (none used)
     */
    public static void main(String[] args) {
        Path dataDir = Paths.get(DATA_DIR_NAME).toAbsolutePath();

        System.out.println("\n[" + getTimestamp() + "] " + APP_NAME + " v" + VERSION + " starting...");
        System.out.println("[" + getTimestamp() + "] Data directory: " + dataDir);

        ClubApplication app = new ClubApplication(dataDir);
        app.initialize();
        app.startMenu();
    }

    /**
     * Initializes the system: creates directories, generates data, seeds merchandise, and pre-loads repositories.
     */
    private void initialize() {
        long startTime = System.currentTimeMillis();

        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            System.err.println("[" + getTimestamp() + "] ERROR: Could not create data directory: " + e.getMessage());
            System.exit(1);
        }

        DataGenerator generator = new DataGenerator(dataDir);

        if (generator.needsGeneration()) {
            System.out.println("[" + getTimestamp() + "] No data found. Generating initial dataset...");
            long genStart = System.currentTimeMillis();
            int records = generator.generateAll();
            long genTime = System.currentTimeMillis() - genStart;
            System.out.println("[" + getTimestamp() + "] Generated " + records + " records in " + genTime + "ms");
        } else {
            System.out.println("[" + getTimestamp() + "] Existing data found. Skipping generation.");
            // Still attempt to seed merchandise catalog if it is missing or empty.
            int seeded = generator.seedMerchandiseIfEmpty();
            if (seeded > 0) {
                System.out.println("[" + getTimestamp() + "] Merchandise catalog seeded with " + seeded + " products.");
            }
        }

        long loadTime = System.currentTimeMillis() - startTime;
        System.out.println("[" + getTimestamp() + "] Initialization complete in " + loadTime + "ms");

        printSystemInfo();
    }

    /**
     * Starts the interactive CLI menu.
     */
    private void startMenu() {
        try {
            SecureMenu menu = new SecureMenu(dataDir);
            menu.start();
        } catch (Exception e) {
            System.err.println("\n[" + getTimestamp() + "] FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Prints system information and statistics.
     */
    private void printSystemInfo() {
        System.out.println("\n  ===== SYSTEM INFORMATION =====");
        System.out.println("  Application:     " + APP_NAME + " v" + VERSION);
        System.out.println("  Java Version:    " + System.getProperty("java.version"));
        System.out.println("  Java Vendor:     " + System.getProperty("java.vendor"));
        System.out.println("  OS:              " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("  Architecture:    " + System.getProperty("os.arch"));
        System.out.println("  Working Dir:     " + Paths.get(".").toAbsolutePath().normalize());
        System.out.println("  Available CPUs:  " + Runtime.getRuntime().availableProcessors());
        System.out.println("  Max Memory:      " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
        System.out.println("  =================================");
    }

    /**
     * Returns a timestamp string for logging.
     */
    private static String getTimestamp() {
        return java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
