package com.club.simulator;

import com.club.model.*;
import com.club.repository.*;
import com.club.service.*;
import com.club.exception.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * High-concurrency stress-test engine for the FCM-ERP ticketing system.
 *
 * <p>Simulates a thundering-herd scenario: up to 2,000 concurrent threads
 * simultaneously attempting to book the same small set of seats.  This
 * deliberately maximises contention to expose any locking deficiencies.</p>
 *
 * <h3>Architecture</h3>
 * <pre>
 * main thread
 *   |-- create ExecutorService (fixed thread pool)
 *   |-- create CountDownLatch(1)  -- start gate
 *   |-- submit N runnable tasks
 *   |-- latch.countDown()         -- all threads fire simultaneously
 *   |-- latch.await()             -- wait for all to finish
 * </pre>
 *
 * <h3>Metrics</h3>
 * <ul>
 *   <li>Throughput: tx/sec = total_attempts / wall_clock_seconds</li>
 *   <li>Success rate: 100 * successful / total</li>
 *   <li>Double-booking rate: 100 * seats_with_duplicate_tickets / target_seats</li>
 * </ul>
 *
 * <h3>Double-Booking Verification</h3>
 * <p>After the run, the simulator counts how many target seats appear more
 * than once in {@code ticket_orders.csv} (or the ticket repository). A count
 * greater than zero means the locking strategy leaked.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class TicketSimulator {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final TicketService ticketService;
    private final SeatRepository seatRepo;
    private final TicketRepository ticketRepo;
    private final MatchRepository matchRepo;
    private final FanRepository fanRepo;
    private final TransactionRepository transactionRepo;

    // =========================================================================
    // CONFIGURATION
    // =========================================================================

    /** Number of concurrent worker threads. 2,000 is the stress-test ceiling. */
    private int threadCount = 500;

    /** Seats each thread attempts to book in one transaction. */
    private int seatsPerThread = 1;

    /** How many times each thread repeats the attack. */
    private int attacksPerThread = 1;

    /** Target seats — each seat in this list is fought over by all threads. */
    private List<String> targetSeatIds = new ArrayList<>();

    /** Match ID used for all simulated bookings. */
    private String matchId = "SIM_MATCH_BENCHMARK";

    // =========================================================================
    // ATOMIC METRICS — thread-safe, lock-free counters
    // =========================================================================

    /** Total booking attempts completed successfully (incrementAndGet per success). */
    private final AtomicInteger successCount = new AtomicInteger(0);

    /** Total booking attempts that threw an exception. */
    private final AtomicInteger failureCount = new AtomicInteger(0);

    /**
     * Number of seats that received more than one confirmed ticket
     * (each increments this counter once regardless of how many duplicates exist).
     */
    private final AtomicInteger doubleBookedSeatCount = new AtomicInteger(0);

    /** Total optimistic-lock retry events across all threads. */
    private final AtomicInteger optimisticRetries = new AtomicInteger(0);

    /** Total count of OptimisticLockException throws after all retries exhausted. */
    private final AtomicInteger optimisticExhaustedCount = new AtomicInteger(0);

    /** Total count of SeatNotAvailableException (expected race outcome). */
    private final AtomicInteger notAvailableCount = new AtomicInteger(0);

    /** Per-thread error messages for diagnostics. */
    private final List<String> errorLog = Collections.synchronizedList(new ArrayList<>());

    // =========================================================================
    // RESULTS
    // =========================================================================

    private long wallClockMs = 0;
    private long startEpochMs = 0;
    private long endEpochMs = 0;

    // =========================================================================
    // CONSTRUCTION
    // =========================================================================

    public TicketSimulator(TicketService ticketService,
                          SeatRepository seatRepo,
                          TicketRepository ticketRepo,
                          MatchRepository matchRepo,
                          FanRepository fanRepo,
                          TransactionRepository transactionRepo) {
        this.ticketService  = ticketService;
        this.seatRepo      = seatRepo;
        this.ticketRepo    = ticketRepo;
        this.matchRepo     = matchRepo;
        this.fanRepo       = fanRepo;
        this.transactionRepo = transactionRepo;
    }

    // =========================================================================
    // CONFIGURATION
    // =========================================================================

    public void setThreadCount(int n) {
        if (n < 1 || n > 2000) throw new IllegalArgumentException(
                "threadCount must be 1..2000: " + n);
        this.threadCount = n;
    }

    public void setSeatsPerThread(int n) {
        if (n < 1 || n > TicketService.MAX_TICKETS_PER_BOOKING) throw new IllegalArgumentException(
                "seatsPerThread must be 1.." + TicketService.MAX_TICKETS_PER_BOOKING + ": " + n);
        this.seatsPerThread = n;
    }

    public void setAttacksPerThread(int n) {
        if (n < 1) throw new IllegalArgumentException("attacksPerThread must be >= 1: " + n);
        this.attacksPerThread = n;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public void setLockMode(TicketService.LockMode mode) {
        ticketService.setLockMode(mode);
    }

    /**
     * Sets the target seats for the simulation.  All threads will compete for
     * these seats, maximising contention.  The list is copied internally so
     * caller changes after this call do not affect the simulation.
     */
    public void setTargetSeats(List<String> seatIds) {
        this.targetSeatIds = new ArrayList<>(seatIds);
    }

    public void setTargetSeats(String... seatIds) {
        this.targetSeatIds = Arrays.asList(seatIds);
    }

    // =========================================================================
    // PRIMARY ENTRY POINT — single-mode run
    // =========================================================================

    /**
     * Executes a single high-concurrency simulation run with the current settings.
     *
     * <h4>Execution Phases</h4>
     * <ol>
     *   <li>Reset all atomic metrics.</li>
     *   <li>Ensure test match exists.</li>
     *   <li>Ensure N test fans exist (one per thread, so every thread has a unique fan).</li>
     *   <li>Reset target seats to AVAILABLE.</li>
     *   <li>Create a fixed-thread-pool {@link ExecutorService}.</li>
     *   <li>Create a {@link CountDownLatch}(1) — the start gate.</li>
     *   <li>Submit all worker tasks; each blocks on {@code latch.await()}.</li>
     *   <li>Fire the latch: {@code latch.countDown()}. All threads desynchronise and
     *       begin booking simultaneously.</li>
     *   <li>Wait for all workers to finish (60s timeout).</li>
     *   <li>Shut down the executor.</li>
     *   <li>Build and return the {@link BenchmarkResult}.</li>
     * </ol>
     *
     * @return the benchmark result with all metrics
     */
    public BenchmarkResult run() {
        resetMetrics();
        ensureTestMatch();
        ensureTestFans();

        System.out.println("\n" + repeat("=", 72));
        System.out.println("          TICKET SIMULATOR — CONCURRENCY STRESS TEST");
        System.out.println(repeat("=", 72));
        System.out.printf("  Lock Mode:        %s%n", ticketService.getLockMode());
        System.out.printf("  Thread Pool:      %,d threads%n", threadCount);
        System.out.printf("  Seats / Thread:   %d%n", seatsPerThread);
        System.out.printf("  Attacks / Thread: %d%n", attacksPerThread);
        System.out.printf("  Target Seats:     %s%n", targetSeatIds);
        System.out.printf("  Match ID:         %s%n", matchId);
        System.out.printf("  Total Attempts:   %,d%n",
                threadCount * attacksPerThread);
        System.out.println(repeat("=", 72));

        // Mark seats AVAILABLE before the thundering herd.
        resetSeatsToAvailable();
        clearTicketsForMatch();

        startEpochMs = System.currentTimeMillis();

        // Fixed thread pool — exactly threadCount workers.
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // The start gate: all threads wait on this latch until we fire it.
        CountDownLatch startGate = new CountDownLatch(1);

        // Completion gate: tracks when all workers have finished.
        CountDownLatch completionGate = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadIdx = i;
            executor.submit(() -> {
                try {
                    // Phase 1: all threads park here simultaneously.
                    startGate.await();

                    // Phase 2: the attack loop.
                    for (int attack = 0; attack < attacksPerThread; attack++) {
                        runAttack(threadIdx, attack);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    recordError("Thread " + threadIdx + " interrupted");
                } finally {
                    completionGate.countDown();
                }
            });
        }

        // FIRE — all parked threads start at the exact same nanosecond.
        startGate.countDown();

        try {
            // Wait up to 60 seconds for all threads to finish.
            if (!completionGate.await(60, TimeUnit.SECONDS)) {
                System.err.println("[Simulator] WARNING: completion timeout — some threads may be deadlocked");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        endEpochMs = System.currentTimeMillis();
        wallClockMs = endEpochMs - startEpochMs;

        BenchmarkResult result = buildResult();

        printResult(result);
        writeResultToFile(result);

        return result;
    }

    // =========================================================================
    // ATTACK LOOP — one thread's work unit
    // =========================================================================

    /**
     * Runs a single booking attack for one thread.
     *
     * <p>Each thread selects {@code seatsPerThread} distinct seats from the
     * {@code targetSeatIds} list using deterministic round-robin assignment so
     * that every attack targets the same seats (maximising contention).</p>
     */
    private void runAttack(int threadIdx, int attackIdx) {
        List<String> seatsToBook = new ArrayList<>(seatsPerThread);

        for (int s = 0; s < seatsPerThread; s++) {
            int seatIdx = (threadIdx + s) % Math.max(1, targetSeatIds.size());
            seatsToBook.add(targetSeatIds.get(seatIdx));
        }

        String fanId = fanIdForThread(threadIdx);
        String processedBy = "SIM[" + threadIdx + "]";

        try {
            ticketService.bookTickets(fanId, matchId, seatsToBook, processedBy);
            successCount.incrementAndGet();

        } catch (OptimisticLockException e) {
            optimisticExhaustedCount.incrementAndGet();
            failureCount.incrementAndGet();

        } catch (SeatNotAvailableException e) {
            notAvailableCount.incrementAndGet();
            failureCount.incrementAndGet();

        } catch (DoubleBookingException e) {
            doubleBookedSeatCount.incrementAndGet();
            failureCount.incrementAndGet();

        } catch (ExceedsMaxTicketsException e) {
            recordError("Thread " + threadIdx + " attack " + attackIdx
                    + ": exceeds max tickets — " + e.getMessage());
            failureCount.incrementAndGet();

        } catch (Exception e) {
            recordError("Thread " + threadIdx + " attack " + attackIdx
                    + ": " + e.getClass().getSimpleName() + " — " + e.getMessage());
            failureCount.incrementAndGet();
        }
    }

    // =========================================================================
    // COMPARATIVE BENCHMARK — all 4 modes sequentially
    // =========================================================================

    /**
     * Runs all four locking modes sequentially on the same target seats and
     * produces a side-by-side ASCII comparison table.
     *
     * <h4>Execution Order</h4>
     * <ol>
     *   <li>Reset every seat in {@code targetSeatIds} to AVAILABLE.</li>
     *   <li>Clear all tickets for the benchmark match.</li>
     *   <li>For each mode: run {@link #run()} with that mode active.</li>
     *   <li>Print the comparative matrix.</li>
     * </ol>
     *
     * @param targetSeats seats every thread will compete for (copied internally)
     * @param threads     number of concurrent threads per mode
     * @return map from mode name to its {@link BenchmarkResult}
     */
    public Map<String, BenchmarkResult> runComparativeBenchmark(
            List<String> targetSeats, int threads) {

        setTargetSeats(targetSeats);
        setThreadCount(threads);

        Map<String, BenchmarkResult> results = new LinkedHashMap<>();

        TicketService.LockMode[] modes = {
                TicketService.LockMode.NO_LOCK,
                TicketService.LockMode.SYNCHRONIZED,
                TicketService.LockMode.FILE_LOCK,
                TicketService.LockMode.OPTIMISTIC
        };

        for (TicketService.LockMode mode : modes) {
            System.out.println("\n>>> ============================================================");
            System.out.println(">>> BENCHMARK MODE: " + mode);
            System.out.println(">>> ============================================================");

            // Reset: clear all tickets and reset seats before next mode.
            clearTicketsForMatch();
            resetSeatsToAvailable();
            clearTransactionHistory();
            setLockMode(mode);

            // Give the OS 500ms to settle between benchmark runs.
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            BenchmarkResult result = run();
            if (result != null) {
                results.put(mode.name(), result);
            }
        }

        printComparisonTable(results);
        return results;
    }

    // =========================================================================
    // RESULT BUILDING
    // =========================================================================

    private BenchmarkResult buildResult() {
        int total = successCount.get() + failureCount.get();
        double durationSec = wallClockMs / 1000.0;

        double throughput = durationSec > 0.001
                ? total / durationSec : 0.0;

        double successRate = total > 0
                ? 100.0 * successCount.get() / total : 0.0;

        int doubleBooked = countDoubleBookedSeats();
        int actualBooked = countActualBookedSeats();

        // Double-booking rate as a percentage of target seats (0..100).
        double doubleBookRate = Math.max(0, targetSeatIds.size()) > 0
                ? 100.0 * doubleBooked / targetSeatIds.size() : 0.0;

        return new BenchmarkResult(
                ticketService.getLockMode().name(),
                threadCount,
                seatsPerThread,
                total,
                successCount.get(),
                failureCount.get(),
                notAvailableCount.get(),
                optimisticExhaustedCount.get(),
                doubleBooked,
                actualBooked,
                wallClockMs,
                throughput,
                successRate,
                doubleBookRate,
                new Date().toString()
        );
    }

    // =========================================================================
    // DOUBLE-BOOKING VERIFICATION
    // =========================================================================

    /**
     * Scans the ticket repository for the benchmark match and counts how many
     * target seats have more than one confirmed ticket issued against them.
     *
     * <p>This is the authoritative double-booking detection algorithm.  Any seat
     * appearing more than once in the ticket table for the same match is a
     * locking failure.</p>
     *
     * <h4>Algorithm</h4>
     * <pre>
     * seatTicketCount = HashMap&lt;String seatId, Integer count&gt;
     * for ticket in ticketRepo.findByMatchId(matchId):
     *     if ticket.seatId in targetSeatIds:
     *         seatTicketCount[seatId]++
     *
     * doubleBookedSeats = count of entries where count &gt; 1
     * </pre>
     *
     * <h4>Time Complexity</h4>
     * <p>O(T + M) where T = targetSeatIds.size() and M = total tickets for the match.
     * Dominated by the {@link TicketRepository#findByMatchId} scan.</p>
     *
     * @return the number of seats with duplicate ticket issuances
     */
    private int countDoubleBookedSeats() {
        Map<String, AtomicInteger> seatCounts = new HashMap<>();
        for (String sid : targetSeatIds) {
            seatCounts.put(sid, new AtomicInteger(0));
        }

        for (Ticket t : ticketRepo.findByMatchId(matchId)) {
            AtomicInteger counter = seatCounts.get(t.getSeatId());
            if (counter != null) {
                counter.incrementAndGet();
            }
        }

        int doubleBooked = 0;
        for (AtomicInteger cnt : seatCounts.values()) {
            if (cnt.get() > 1) {
                doubleBooked++;
            }
        }
        return doubleBooked;
    }

    /**
     * Counts how many of the target seats are currently in BOOKED state.
     */
    private int countActualBookedSeats() {
        int count = 0;
        for (String seatId : targetSeatIds) {
            Seat seat = seatRepo.findById(seatId);
            if (seat != null && seat.getStatus() == SeatStatus.BOOKED) {
                count++;
            }
        }
        return count;
    }

    // =========================================================================
    // PRINTS & OUTPUT
    // =========================================================================

    private void printResult(BenchmarkResult r) {
        String safety = r.doubleBookRate == 0
                ? "  [SAFE — no double bookings detected]"
                : "  [DANGER — double bookings detected: " + r.doubleBookedSeats + " seats]";

        System.out.println("\n" + repeat("-", 72));
        System.out.println("  BENCHMARK RESULT  |  " + r.lockMode);
        System.out.println(repeat("-", 72));
        System.out.printf("  Threads:            %,12d%n", r.threadCount);
        System.out.printf("  Seats/Thread:      %,12d%n", r.seatsPerThread);
        System.out.printf("  Total Attempts:    %,12d%n", r.totalAttempts);
        System.out.printf("  Wall Clock:        %,12d ms%n", r.wallClockMs);
        System.out.printf("  Throughput:        %11.2f tx/sec%n", r.throughput);
        System.out.println(repeat("-", 72));
        System.out.printf("  Successful:        %,12d  (%6.2f %%)%n",
                r.successfulBookings, r.successRate);
        System.out.printf("  Failed:            %,12d%n", r.failedBookings);
        System.out.printf("    Seat Not Avail:  %,12d%n", r.notAvailableCount);
        System.out.printf("    Optimistic Fail:%,12d%n", r.optimisticExhausted);
        System.out.printf("    Double Bookings: %,12d%n", r.doubleBookedSeats);
        System.out.printf("    Other Errors:   %,12d%n",
                r.failedBookings - r.notAvailableCount
                        - r.optimisticExhausted - r.doubleBookedSeats);
        System.out.println(repeat("-", 72));
        System.out.printf("  Double-Book Rate:  %11.2f %% %s%n", r.doubleBookRate, safety);
        System.out.printf("  Actual Booked:    %,12d / %,d%n",
                r.actualBookedSeats, targetSeatIds.size());
        System.out.println(repeat("=", 72));
    }

    /**
     * Prints a four-mode comparative ASCII matrix with all key metrics.
     */
    public void printComparisonTable(Map<String, BenchmarkResult> results) {
        String[] headers = {
                "Mode",
                "Threads",
                "Wall(ms)",
                "Throughput",
                "Success%",
                "DB Rate%",
                "Actual",
                "Safety"
        };

        int[] widths = {14, 9, 10, 12, 9, 10, 8, 8};
        String fmt = "  %-" + widths[0] + "s"
                + " %" + widths[1] + "s"
                + " %" + widths[2] + "s"
                + " %" + widths[3] + "s"
                + " %" + widths[4] + "s"
                + " %" + widths[5] + "s"
                + " %" + widths[6] + "s"
                + " %" + widths[7] + "s%n";

        String sep = "  " + repeat("-", widths[0] + 1)
                + repeat("-", widths[1] + 1)
                + repeat("-", widths[2] + 1)
                + repeat("-", widths[3] + 1)
                + repeat("-", widths[4] + 1)
                + repeat("-", widths[5] + 1)
                + repeat("-", widths[6] + 1)
                + repeat("-", widths[7]);

        System.out.println("\n" + repeat("=", 80));
        System.out.println("                    CONCURRENCY BENCHMARK COMPARISON MATRIX");
        System.out.println(repeat("=", 80));
        System.out.printf(fmt, (Object[]) headers);
        System.out.println(sep);

        for (BenchmarkResult r : results.values()) {
            String safety = r.doubleBookRate == 0 ? "SAFE" : "LEAKED";
            System.out.printf(fmt,
                    r.lockMode,
                    r.threadCount,
                    r.wallClockMs,
                    String.format("%.2f", r.throughput),
                    String.format("%.2f", r.successRate),
                    String.format("%.2f", r.doubleBookRate),
                    r.actualBookedSeats + "/" + targetSeatIds.size(),
                    safety);
        }

        System.out.println(repeat("=", 80));

        // Safety summary
        System.out.println("\n  SAFETY SUMMARY:");
        for (BenchmarkResult r : results.values()) {
            String icon = r.doubleBookRate == 0 ? "[OK]" : "[!!]";
            System.out.printf("  %s %-14s  DB rate: %6.2f %%   Throughput: %8.2f tx/s%n",
                    icon, r.lockMode, r.doubleBookRate, r.throughput);
        }
        System.out.println();
    }

    private void writeResultToFile(BenchmarkResult r) {
        Path out = Paths.get("benchmark_results.csv");
        boolean exists = Files.exists(out);
        try {
            String line = String.format("%s,%d,%d,%d,%d,%d,%d,%d,%.2f,%.2f,%.2f,%d,%s%n",
                    r.lockMode, r.threadCount, r.seatsPerThread,
                    r.totalAttempts, r.successfulBookings, r.failedBookings,
                    r.notAvailableCount, r.optimisticExhausted,
                    r.throughput, r.successRate, r.doubleBookRate,
                    r.wallClockMs, new Date().toString());

            Files.write(out,
                    (exists ? "" : "mode,threads,seatsPerThread,total,success,fail,notAvail,optFail,throughput,successRate,doubleBookRate,wallClockMs,timestamp\n")
                            .getBytes(StandardCharsets.UTF_8),
                    exists ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);

            Files.write(out, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);

        } catch (IOException e) {
            System.err.println("[Simulator] Could not write benchmark_results.csv: "
                    + e.getMessage());
        }
    }

    // =========================================================================
    // TEST FIXTURE MANAGEMENT
    // =========================================================================

    private void ensureTestMatch() {
        if (matchRepo.findById(matchId) == null) {
            Match m = new Match();
            m.setMatchId(matchId);
            m.setHomeTeam("HOME FC");
            m.setAwayTeam("AWAY FC");
            m.setVenue("Stadium");
            m.setMatchDate("2026-06-01");
            m.setMatchTime("15:00");
            m.setStatus(MatchStatus.TICKETS_ON_SALE);
            m.setTicketPriceStandard(50.0);
            m.setTicketPriceVip(200.0);
            try { matchRepo.save(m); } catch (IOException e) { /* best-effort */ }
        }
    }

    /**
     * Ensures at least {@code threadCount} test fans exist, one per thread.
     * Each fan starts with a large balance so no booking is rejected for funds.
     */
    private void ensureTestFans() {
        for (int i = 0; i < threadCount; i++) {
            String fanId = fanIdForThread(i);
            Fan fan = fanRepo.findById(fanId);
            if (fan == null) {
                fan = new Fan();
                fan.setId(fanId);
                fan.setName("SimFan_" + String.format("%05d", i));
                fan.setEmail("simfan" + i + "@benchmark.local");
                fan.setAccountBalance(1_000_000.0); // large enough for any test
                try { fanRepo.save(fan); } catch (IOException e) { /* best-effort */ }
            } else {
                fan.setAccountBalance(1_000_000.0);
                try { fanRepo.save(fan); } catch (IOException e) { /* best-effort */ }
            }
        }
    }

    /**
     * Resets every target seat to AVAILABLE and version 1, then flushes to disk.
     */
    private void resetSeatsToAvailable() {
        for (String seatId : targetSeatIds) {
            Seat seat = seatRepo.findById(seatId);
            if (seat != null) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setVersion(1);
                try { seatRepo.save(seat); } catch (IOException e) { /* best-effort */ }
            }
        }
        try { seatRepo.flush(); } catch (IOException e) { /* best-effort */ }
    }

    /** Deletes all tickets for the benchmark match to start the simulation clean. */
    private void clearTicketsForMatch() {
        List<Ticket> existing = ticketRepo.findByMatchId(matchId);
        for (Ticket t : existing) {
            try {
                // Release the seat first.
                Seat seat = seatRepo.findById(t.getSeatId());
                if (seat != null && seat.getStatus() == SeatStatus.BOOKED) {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setVersion(1);
                    seatRepo.cacheOnly(seat);
                }
                ticketRepo.deleteById(t.getTicketId());
            } catch (IOException e) { /* best-effort */ }
        }
        try { seatRepo.flush(); } catch (IOException e) { /* best-effort */ }
    }

    /** Clears transaction history generated by the simulator. */
    private void clearTransactionHistory() {
        for (Transaction t : transactionRepo.findAll()) {
            try { transactionRepo.deleteById(t.getTransactionId()); } catch (IOException ignored) {}
        }
    }

    private void resetMetrics() {
        successCount.set(0);
        failureCount.set(0);
        doubleBookedSeatCount.set(0);
        optimisticRetries.set(0);
        optimisticExhaustedCount.set(0);
        notAvailableCount.set(0);
        errorLog.clear();
        wallClockMs = 0;
    }

    private void recordError(String msg) {
        errorLog.add("[" + System.currentTimeMillis() + "] " + msg);
    }

    private String fanIdForThread(int threadIdx) {
        return "SIM_FAN_" + String.format("%05d", threadIdx);
    }

    private String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(count * s.length());
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }

    // =========================================================================
    // INNER CLASS — BenchmarkResult
    // =========================================================================

    /**
     * Immutable container for all metrics from a single simulation run.
     * All fields are final and set by the constructor — safe to share across
     * threads without synchronisation.
     */
    public static class BenchmarkResult {

        public final String lockMode;
        public final int threadCount;
        public final int seatsPerThread;
        public final int totalAttempts;
        public final int successfulBookings;
        public final int failedBookings;
        public final int notAvailableCount;
        public final int optimisticExhausted;
        public final int doubleBookedSeats;
        public final int actualBookedSeats;
        public final long wallClockMs;
        public final double throughput;
        public final double successRate;
        public final double doubleBookRate;
        public final String timestamp;

        public BenchmarkResult(String lockMode, int threadCount, int seatsPerThread,
                             int totalAttempts, int successfulBookings, int failedBookings,
                             int notAvailableCount, int optimisticExhausted,
                             int doubleBookedSeats, int actualBookedSeats,
                             long wallClockMs, double throughput, double successRate,
                             double doubleBookRate, String timestamp) {
            this.lockMode           = lockMode;
            this.threadCount        = threadCount;
            this.seatsPerThread    = seatsPerThread;
            this.totalAttempts      = totalAttempts;
            this.successfulBookings = successfulBookings;
            this.failedBookings    = failedBookings;
            this.notAvailableCount = notAvailableCount;
            this.optimisticExhausted = optimisticExhausted;
            this.doubleBookedSeats  = doubleBookedSeats;
            this.actualBookedSeats = actualBookedSeats;
            this.wallClockMs       = wallClockMs;
            this.throughput        = throughput;
            this.successRate       = successRate;
            this.doubleBookRate    = doubleBookRate;
            this.timestamp         = timestamp;
        }

        public String toCsvLine() {
            return String.format("%s,%d,%d,%d,%d,%d,%d,%d,%.2f,%.2f,%.2f,%d,%s",
                    lockMode, threadCount, seatsPerThread,
                    totalAttempts, successfulBookings, failedBookings,
                    notAvailableCount, optimisticExhausted,
                    throughput, successRate, doubleBookRate,
                    wallClockMs, timestamp);
        }

        public static String csvHeader() {
            return "mode,threadCount,seatsPerThread,total,success,fail,"
                    + "notAvailable,optFail,throughput,successRate,doubleBookRate,"
                    + "wallClockMs,timestamp";
        }

        @Override
        public String toString() {
            return String.format(
                    "BenchmarkResult[%s]: throughput=%.2f tx/s, successRate=%.2f%%, doubleBookRate=%.2f%%",
                    lockMode, throughput, successRate, doubleBookRate);
        }
    }
}
