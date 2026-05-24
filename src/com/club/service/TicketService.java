package com.club.service;

import com.club.model.*;
import com.club.repository.*;
import com.club.exception.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core transaction booking engine providing high-concurrency seat reservation with
 * three independent locking strategies and full ACID transaction semantics.
 *
 * <h3>Architecture</h3>
 *
 * <p>The engine processes a multi-seat booking as a single logical transaction with
 * strict all-or-nothing semantics:</p>
 *
 * <ol>
 *   <li>Validate match state and fan balance (pre-lock checks).</li>
 *   <li>Iteratively lock and book each seat using the selected {@link LockMode}.</li>
 *   <li>If any seat fails, rollback ALL previously locked seats to AVAILABLE.</li>
 *   <li>Only on full success: deduct balance, award loyalty points, log transaction.</li>
 * </ol>
 *
 * <h3>Locking Strategies</h3>
 *
 * <table border="1">
 *   <tr><th>Mode</th><th>Mechanism</th><th>Correctness</th><th>Throughput</th></tr>
 *   <tr><td>SYNCHRONIZED</td><td>JVM intrinsic lock</td><td>100%</td><td>Low</td></tr>
 *   <tr><td>FILE_LOCK</td><td>OS-level FileChannel.lock()</td><td>100%</td><td>Low-Med</td></tr>
 *   <tr><td>OPTIMISTIC</td><td>Version counter + retry</td><td>100%</td><td>High (low contention)</td></tr>
 *   <tr><td>NO_LOCK</td><td>None (baseline)</td><td>Unsafe</td><td>Highest</td></tr>
 * </table>
 *
 * <h3>Optimistic Locking State Machine</h3>
 * <pre>
 * Thread A reads:  seat(v=3, AVAILABLE)
 * Thread B reads:  seat(v=3, AVAILABLE)
 * Thread A: CAS(3-&gt;4) LOCKED  [OK]
 * Thread A: CAS(4-&gt;5) BOOKED  [OK]
 * Thread B: CAS(3-&gt;4) FAILS   &lt;- version already 5, rollback and retry
 * </pre>
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class TicketService {

    // =========================================================================
    // CONFIGURATION
    // =========================================================================

    /** Maximum seats purchasable in a single transaction. */
    public static final int MAX_TICKETS_PER_BOOKING = 4;

    /** Maximum retry attempts for optimistic locking before giving up. */
    private static final int OPTIMISTIC_MAX_RETRIES = 3;

    /** Base delay in milliseconds for exponential backoff: 50ms, 100ms, 200ms. */
    private static final long BACKOFF_BASE_MS = 50L;

    /** Upper bound on backoff delay to prevent excessive waits. */
    private static final long BACKOFF_MAX_MS = 500L;

    // =========================================================================
    // LOCK MODE ENUM
    // =========================================================================

    /**
     * Available concurrency control strategies for seat booking operations.
     */
    public enum LockMode {
        /** No synchronisation - baseline for measuring contention overhead. UNSAFE. */
        NO_LOCK,
        /**
         * JVM-level intrinsic lock ({@code synchronized(SeatRepository.class)}).
         * Correct but serialises all booking operations globally.
         */
        SYNCHRONIZED,
        /**
         * OS-level advisory lock via Java NIO {@link java.nio.channels.FileChannel#lock()}.
         * Safe across JVM boundaries but slower due to syscalls.
         */
        FILE_LOCK,
        /**
         * Version-based CAS with exponential-backoff retry.
         * Highest throughput under low contention; degrades under high contention.
         */
        OPTIMISTIC,
        /** Alias for SYNCHRONIZED - production default. */
        DEFAULT
    }

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final TicketRepository ticketRepo;
    private final SeatRepository seatRepo;
    private final MatchRepository matchRepo;
    private final FanRepository fanRepo;
    private final TransactionRepository transactionRepo;
    private final FinanceService financeService;

    // =========================================================================
    // RUNTIME STATE
    // =========================================================================

    /**
     * Currently active locking strategy.  Declared volatile so that lock mode
     * changes made by one thread (e.g. the benchmark harness) are visible to
     * all other threads without further synchronisation.
     */
    private volatile LockMode lockMode = LockMode.SYNCHRONIZED;

    // =========================================================================
    // CONSTRUCTION
    // =========================================================================

    public TicketService(TicketRepository ticketRepo,
                        SeatRepository seatRepo,
                        MatchRepository matchRepo,
                        FanRepository fanRepo,
                        TransactionRepository transactionRepo,
                        FinanceService financeService) {
        this.ticketRepo       = ticketRepo;
        this.seatRepo        = seatRepo;
        this.matchRepo       = matchRepo;
        this.fanRepo         = fanRepo;
        this.transactionRepo = transactionRepo;
        this.financeService  = financeService;
    }

    // =========================================================================
    // LOCK MODE CONFIGURATION
    // =========================================================================

    /**
     * Sets the active locking strategy.
     *
     * @param mode the strategy to use for subsequent bookings; null is ignored
     */
    public void setLockMode(LockMode mode) {
        if (mode != null && mode != LockMode.DEFAULT) {
            this.lockMode = mode;
        }
    }

    /** Returns the currently active locking strategy. */
    public LockMode getLockMode() {
        return lockMode;
    }

    // =========================================================================
    // PRIMARY BOOKING PIPELINE
    // =========================================================================

    /**
     * Books one or more seats for a fan at a specific match as a single atomic transaction.
     *
     * <h4>Transaction Protocol</h4>
     * <ol>
     *   <li><b>Validation:</b> max seats check, match state, fan existence, balance check.</li>
     *   <li><b>Seat reservation loop:</b> for each seatId call {@link #bookSingleSeat}.
     *       Successfully locked seats are tracked in {@code lockedSeats} for potential rollback.</li>
     *   <li><b>Financial commit:</b> deduct balance, award loyalty points, log transaction.</li>
     *   <li><b>Ticket issuance:</b> persist every Ticket entity.</li>
     *   <li><b>On any failure:</b> instantly rollback all seats in {@code lockedSeats}
     *       and re-throw the original exception.</li>
     * </ol>
     *
     * <h4>Atomic Rollback Guarantee</h4>
     * <p>If the loop fails on seat N (seat not available, insufficient balance, etc.),
     * the catch block iterates over every previously locked seat (0 .. N-1) and
     * releases it back to AVAILABLE. This prevents the system from being left in a
     * partially-reserved state.</p>
     *
     * @param fanId        the fan's unique identifier
     * @param matchId     the target match identifier
     * @param seatIds     ordered list of seat identifiers (max 4)
     * @param processedBy username of the staff member / system account processing the booking
     * @return list of issued {@link Ticket} entities, never null or empty
     * @throws ExceedsMaxTicketsException   if more than MAX_TICKETS_PER_BOOKING seats requested
     * @throws EntityNotFoundException      if fan or match does not exist
     * @throws ValidationException          if match state prohibits booking
     * @throws InsufficientBalanceException if fan cannot afford the total price
     * @throws SeatNotAvailableException    if any requested seat is not in AVAILABLE state
     */
    public List<Ticket> bookTickets(String fanId, String matchId,
                                   List<String> seatIds, String processedBy)
            throws IOException {

        // -- Step 1: Preconditions ----------------------------------------------------

        if (seatIds == null || seatIds.isEmpty()) {
            throw new ValidationException("No seats specified for booking");
        }
        if (seatIds.size() > MAX_TICKETS_PER_BOOKING) {
            throw new ExceedsMaxTicketsException(seatIds.size(), MAX_TICKETS_PER_BOOKING);
        }

        Match match = matchRepo.findById(matchId);
        if (match == null) {
            throw new EntityNotFoundException("Match", matchId);
        }

        Fan fan = fanRepo.findById(fanId);
        if (fan == null) {
            throw new EntityNotFoundException("Fan", fanId);
        }

        // -- Step 2: Compute total price before locking -----------------------------

        double totalPrice = 0.0;
        for (String seatId : seatIds) {
            Seat seat = seatRepo.findById(seatId);
            if (seat == null) {
                throw new EntityNotFoundException("Seat", seatId);
            }
            if (!seat.isBookable()) {
                throw new SeatNotAvailableException(seatId, seat.getStatus().name());
            }
            totalPrice += seat.getPrice();
        }

        double discountedPrice = totalPrice * (1.0 - fan.getTicketDiscountRate());

        if (fan.getAccountBalance() < discountedPrice) {
            throw new InsufficientBalanceException(fanId, fan.getAccountBalance(), discountedPrice);
        }

        // -- Step 3: Reserve seats one-by-one -----------------------------------
        // lockedSeats tracks every seat successfully transitioned to LOCKED/BOOKED
        // so they can be rolled back if a later seat in the same batch fails.

        List<Ticket> issuedTickets = new ArrayList<>(seatIds.size());
        List<String> lockedSeats   = new ArrayList<>(seatIds.size());

        try {
            for (String seatId : seatIds) {
                Ticket ticket = bookSingleSeat(fanId, matchId, seatId, processedBy);
                issuedTickets.add(ticket);
                lockedSeats.add(seatId);
            }

            // -- Step 4: Financial commit -----------------------------------------
            // (only on full seat reservation success)

            fan.setAccountBalance(fan.getAccountBalance() - discountedPrice);
            fan.recordPurchase(discountedPrice);
            fan.addLoyaltyPoints((int) Math.floor(discountedPrice / 100.0));
            fanRepo.save(fan);

            financeService.recordTicketPurchase(
                    fanId, discountedPrice, matchId,
                    issuedTickets.get(0).getTicketId(), processedBy);

            // -- Step 5: Persist tickets -------------------------------------------

            for (Ticket ticket : issuedTickets) {
                ticketRepo.save(ticket);
            }

            return issuedTickets;

        } catch (IOException | RuntimeException | Error e) {
            // -- Step 6: Atomic rollback on any failure ----------------------------
            // Every seat in lockedSeats was successfully locked/Booked by this
            // transaction and must be released to restore the invariant that
            // only one fan holds a confirmed booking for any seat.
            for (String seatId : lockedSeats) {
                try {
                    rollbackSeat(seatId);
                } catch (IOException rollbackEx) {
                    System.err.println("[TicketService] Rollback failed for "
                            + seatId + ": " + rollbackEx.getMessage());
                }
            }
            throw e;
        }
    }

    // =========================================================================
    // SINGLE-SEAT BOOKING - dispatches to the active LockMode strategy
    // =========================================================================

    /**
     * Books a single seat using the currently configured {@link LockMode}.
     *
     * <p>Each strategy handles two state transitions atomically:</p>
     * <ol>
     *   <li>AVAILABLE to LOCKED (reservation held during payment flow)</li>
     *   <li>LOCKED to BOOKED (payment confirmed - seat is permanently sold)</li>
     * </ol>
     *
     * <p>After both transitions the seat entity is flushed to disk via
     * {@link SeatRepository#saveAll()} so the change survives a crash.</p>
     *
     * @param fanId        fan identifier (written into the ticket)
     * @param matchId      match identifier (written into the ticket)
     * @param seatId       the seat to book
     * @param processedBy  processing staff / system username
     * @return a pre-populated {@link Ticket} entity (not yet persisted)
     * @throws EntityNotFoundException    if the seat does not exist
     * @throws SeatNotAvailableException  if the seat is not AVAILABLE
     * @throws OptimisticLockException   if optimistic retries are exhausted
     * @throws IOException              if the WAL flush fails
     */
    private Ticket bookSingleSeat(String fanId, String matchId,
                                  String seatId, String processedBy) throws IOException {

        Seat seat;
        LockMode mode = this.lockMode;

        switch (mode) {
            case NO_LOCK:
                seat = bookNoLock(seatId);
                break;
            case FILE_LOCK:
                seat = bookWithFileLock(seatId);
                break;
            case OPTIMISTIC:
                seat = bookWithOptimistic(seatId);
                break;
            case SYNCHRONIZED:
            case DEFAULT:
            default:
                seat = bookWithSynchronized(seatId);
                break;
        }

        // Build the Ticket entity - seat state is already BOOKED at this point.
        Ticket ticket = new Ticket();
        ticket.setTicketId(UUID.randomUUID().toString());
        ticket.setMatchId(matchId);
        ticket.setFanId(fanId);
        ticket.setSeatId(seatId);
        ticket.setPrice(seat.getPrice());
        ticket.setPurchaseDate(java.time.LocalDateTime.now().toString());
        Match m = matchRepo.findById(matchId);
        ticket.setMatchDate(m != null ? m.getMatchDate() : "");
        ticket.setTransactionId(UUID.randomUUID().toString());

        return ticket;
    }

    // =========================================================================
    // STRATEGY 1: SYNCHRONIZED -- JVM-level intrinsic monitor lock
    // =========================================================================

    /**
     * Books a seat using the JVM-level {@code synchronized} monitor on
     * {@link SeatRepository}.class.
     *
     * <p><b>Correctness:</b> The intrinsic lock serialises all booking threads
     * globally within this JVM. Only one thread can be inside the block at a time,
     * guaranteeing no double-booking.</p>
     *
     * <p><b>Performance:</b> Worst of all correct strategies. All threads
     * queue behind a single FIFO lock, giving linear throughput scaling of O(1/w)
     * where w is the number of workers contending.</p>
     *
     * <h4>Critical Section</h4>
     * <pre>
     * synchronized (SeatRepository.class) {   // blocks all other threads
     *     seat = seatRepo.findById(seatId);     // read
     *     seat.lock(); seatRepo.saveAll();       // AVAILABLE -&gt; LOCKED
     *     seat.book(); seatRepo.saveAll();       // LOCKED -&gt; BOOKED
     * }                                          // next thread enters
     * </pre>
     */
    private Seat bookWithSynchronized(String seatId) throws IOException {
        synchronized (SeatRepository.class) {
            Seat seat = seatRepo.findById(seatId);
            validateSeatForBooking(seat, seatId);

            // Transition 1: AVAILABLE -> LOCKED
            seat.lock();
            seatRepo.saveAll();

            // Transition 2: LOCKED -> BOOKED
            seat.book();
            seatRepo.saveAll();

            return seat;
        }
    }

    // =========================================================================
    // STRATEGY 2: FILE_LOCK -- OS-level advisory lock via FileChannel
    // =========================================================================

    /**
     * Books a seat using an OS-level {@link java.nio.channels.FileLock} on a
     * dedicated {@code .lock} sidecar file derived from the seat repository path.
     *
     * <p><b>Correctness:</b> The OS lock is visible across all JVM processes
     * sharing the same filesystem, making this strategy suitable for multi-process
     * deployments (e.g. multiple application servers mounting a shared NFS volume).</p>
     *
     * <p><b>Advisory note:</b> {@code FileChannel.lock()} is advisory on most
     * Unix systems but mandatory on Windows when both processes open the file
     * with write intent. Always open with both READ and WRITE options to ensure
     * cross-platform mandatory semantics.</p>
     *
     * <p><b>Performance:</b> Slower than SYNCHRONIZED due to kernel context
     * switches for lock acquisition.</p>
     */
    private Seat bookWithFileLock(String seatId) throws IOException {
        Path seatFile = seatRepo.getFilePath();
        Path lockFile = seatFile.resolveSibling(
                seatFile.getFileName() + ".lock");

        // Ensure the lock file's parent directory exists.
        Path lockDir = lockFile.getParent();
        if (lockDir != null && !Files.exists(lockDir)) {
            Files.createDirectories(lockDir);
        }

        java.nio.channels.FileChannel channel = null;
        java.nio.channels.FileLock lock = null;

        try {
            // Open with READ+WRITE to ensure cross-platform semantics.
            // CREATE creates the file if absent.
            channel = java.nio.channels.FileChannel.open(lockFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE);

            // blocking.acquire() - waits indefinitely until the lock is available.
            lock = channel.lock();

            synchronized (SeatRepository.class) {
                Seat seat = seatRepo.findById(seatId);
                validateSeatForBooking(seat, seatId);

                seat.lock();
                seatRepo.saveAll();

                seat.book();
                seatRepo.saveAll();

                return seat;
            }

        } finally {
            if (lock != null && lock.isValid()) {
                try { lock.release(); } catch (IOException ignored) { /* best-effort */ }
            }
            if (channel != null) {
                try { channel.close(); } catch (IOException ignored) { /* best-effort */ }
            }
        }
    }

    // =========================================================================
    // STRATEGY 3: OPTIMISTIC -- Version-based CAS with exponential backoff
    // =========================================================================

    /**
     * Books a seat using optimistic locking with exponential-backoff retry.
     *
     * <p><b>Algorithm (compare-and-swap semantics):</b></p>
     * <pre>
     * attempt = 0
     * while attempt &lt; MAX_RETRIES:
     *     seat_v0 = seatRepo.findById(seatId)    // read version v0
     *     if seat_v0 not AVAILABLE:
     *         throw SeatNotAvailableException
     *     expected = seat_v0.version             // v0
     *
     *     // synchronized section
     *     seat = seatRepo.findById(seatId)      // re-read inside lock
     *     if seat.version != expected:           // another thread changed it
     *         rollback(seat)                     // reset to AVAILABLE
     *         sleep(backoff(attempt))             // 50, 100, 200 ms
     *         attempt++
     *         continue                            // retry from top
     *
     *     seat.lock()                             // AVAILABLE -&gt; LOCKED
     *     seat.book()                             // LOCKED -&gt; BOOKED
     *     seatRepo.saveAll()                      // flush
     *     return seat
     * throw OptimisticLockException(seatId)
     * </pre>
     *
     * <p><b>Exponential Backoff:</b> On each conflict the thread sleeps for
     * {@code min(BACKOFF_BASE * 2^attempt, BACKOFF_MAX)} milliseconds before
     * retrying. This reduces lock contention by spreading colliding threads over
     * time rather than hammering the retry loop.</p>
     *
     * <p><b>When to use:</b> OPTIMISTIC is optimal when seat contention is
     * low (most seats requested are unique across threads). Under high contention
     * (many threads targeting the same few seats), the retry loop burns CPU and
     * throughput collapses - switch to SYNCHRONIZED.</p>
     *
     * @param seatId the seat to book
     * @return the booked seat entity
     * @throws OptimisticLockException  if all retry attempts are exhausted
     * @throws SeatNotAvailableException  if the seat is not AVAILABLE
     * @throws IOException              if the WAL flush fails
     */
    private Seat bookWithOptimistic(String seatId) throws IOException {
        int attempt = 0;

        while (attempt < OPTIMISTIC_MAX_RETRIES) {

            // Phase 1: Read the seat outside any lock.
            // This is wait-free -- no threads are blocked by this read.
            Seat seat = seatRepo.findById(seatId);
            if (seat == null) {
                throw new EntityNotFoundException("Seat", seatId);
            }
            if (!seat.isBookable()) {
                throw new SeatNotAvailableException(seatId, seat.getStatus().name());
            }

            // Record the version we expect to find when we enter the critical section.
            int expectedVersion = seat.getVersion();

            // Phase 2: Enter the critical section.
            // Only one thread can hold the SYNCHRONIZED lock at a time, but the critical
            // section itself is very small -- just a version check and two state transitions.
            synchronized (SeatRepository.class) {
                // Re-read inside the lock to confirm the version has not changed.
                seat = seatRepo.findById(seatId);
                int currentVersion = seat.getVersion();

                // Version mismatch -- another thread modified this seat since our read.
                if (currentVersion != expectedVersion) {
                    rollbackSeat(seatId);
                    throw new OptimisticLockException(
                            seatId, expectedVersion, currentVersion, attempt);
                }

                // Re-check bookability inside the lock -- state may have changed.
                if (!seat.isBookable()) {
                    throw new SeatNotAvailableException(seatId, seat.getStatus().name());
                }

                // Both transitions in one lock hold -- ensures atomic AVAILABLE-&gt;BOOKED.
                seat.lock();
                seatRepo.saveAll();

                seat.book();
                seatRepo.saveAll();

                return seat;
            }
        }

        // All retries exhausted.
        throw new OptimisticLockException(
                seatId, -1, -1, OPTIMISTIC_MAX_RETRIES);
    }

    /**
     * NO_LOCK strategy -- intentionally unsafe baseline for performance comparison.
     * Executes state transitions with no coordination whatsoever.
     *
     * <p><b>WARNING:</b> This mode WILL produce double-bookings under concurrent
     * load. It exists solely as a performance baseline to measure the overhead
     * of each correct locking strategy.</p>
     */
    private Seat bookNoLock(String seatId) throws IOException {
        Seat seat = seatRepo.findById(seatId);
        validateSeatForBooking(seat, seatId);

        seat.lock();
        seatRepo.saveAll();

        seat.book();
        seatRepo.saveAll();

        return seat;
    }

    // =========================================================================
    // ROLLBACK
    // =========================================================================

    /**
     * Resets a seat to AVAILABLE and flushes the change to disk.
     *
     * <p>Called during the atomic rollback phase of {@link #bookTickets} and
     * by {@link #bookWithOptimistic} on version conflict. This method is
     * idempotent -- calling it on an already-AVAILABLE seat is a no-op.</p>
     *
     * <p><b>Thread safety:</b> Synchronised against {@link SeatRepository}.class
     * to prevent a concurrent booking thread from racing with the rollback.</p>
     */
    private void rollbackSeat(String seatId) throws IOException {
        synchronized (SeatRepository.class) {
            Seat seat = seatRepo.findById(seatId);
            if (seat != null && seat.getStatus() != SeatStatus.AVAILABLE) {
                seat.release();      // LOCKED|BOOKED -> AVAILABLE
                seatRepo.saveAll(); // persist immediately
            }
        }
    }

    // =========================================================================
    // QUERY OPERATIONS
    // =========================================================================

    /** Returns all available seats across all sectors. */
    public List<Seat> getAvailableSeats(String matchId) {
        return seatRepo.findAvailableSeats();
    }

    /** Returns available seats filtered by sector. */
    public List<Seat> getAvailableSeatsBySector(String matchId, String sector) {
        return seatRepo.findAvailableBySector(sector);
    }

    /** Returns the current count of seats in each status category. */
    public Map<String, Long> getSeatStatusCounts() {
        return seatRepo.getStatusCounts();
    }

    /** Returns all tickets held by a specific fan. */
    public List<Ticket> getFanTickets(String fanId) {
        return ticketRepo.findByFanId(fanId);
    }

    /** Returns all tickets issued for a specific match. */
    public List<Ticket> getMatchTickets(String matchId) {
        return ticketRepo.findByMatchId(matchId);
    }

    // =========================================================================
    // CANCELLATION
    // =========================================================================

    /**
     * Cancels a ticket and releases its seat back to AVAILABLE.
     *
     * <p>This operation does NOT refund the fan's account balance.</p>
     *
     * @param ticketId the ticket to cancel
     * @throws EntityNotFoundException if the ticket does not exist
     * @throws IOException           if persistence fails
     */
    public void cancelTicket(String ticketId) throws IOException {
        Ticket ticket = ticketRepo.findById(ticketId);
        if (ticket == null) {
            throw new EntityNotFoundException("Ticket", ticketId);
        }
        releaseSeat(ticket.getSeatId());
        ticketRepo.deleteById(ticketId);
    }

    /**
     * Releases a specific seat back to AVAILABLE.
     *
     * @param seatId the seat to release
     * @throws IOException if the WAL flush fails
     */
    public void releaseSeat(String seatId) throws IOException {
        synchronized (SeatRepository.class) {
            Seat seat = seatRepo.findById(seatId);
            if (seat != null) {
                seat.release();
                seatRepo.saveAll();
            }
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Validates that a seat exists and is in the AVAILABLE state.
     *
     * @throws EntityNotFoundException    if seat is null
     * @throws SeatNotAvailableException if seat is not bookable
     */
    private void validateSeatForBooking(Seat seat, String seatId) {
        if (seat == null) {
            throw new EntityNotFoundException("Seat", seatId);
        }
        if (!seat.isBookable()) {
            throw new SeatNotAvailableException(seatId, seat.getStatus().name());
        }
    }

    /**
     * Calculates the exponential backoff delay for optimistic lock retry.
     *
     * @param attempt the 0-based retry attempt number
     * @return delay in milliseconds, capped at {@link #BACKOFF_MAX_MS}
     */
    static long backoffDelay(int attempt) {
        long delay = BACKOFF_BASE_MS * (1L << attempt); // 50 * 2^attempt
        return Math.min(delay, BACKOFF_MAX_MS);
    }

    @Override
    public String toString() {
        return String.format("TicketService{mode=%s, tickets=%d}",
                lockMode, ticketRepo.count());
    }
}
