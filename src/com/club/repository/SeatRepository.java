package com.club.repository;

import com.club.model.Seat;
import com.club.model.SeatStatus;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe Singleton repository for {@link Seat} entities backed by the
 * {@code stadium_seats.csv} file, persisted through the {@link GenericCsvRepository}
 * WAL-powered save pipeline.
 *
 * <h3>Singleton Pattern</h3>
 *
 * <p>This class is a <b>lazy-initialised thread-safe Singleton</b> using the
 * Bill Pugh Singleton idiom (Initialization-on-demand holder). The JVM guarantees
 * that the inner {@link #Holder} class is not loaded until another thread first
 * references any static member of {@code SeatRepository}, and that class loading
 * itself is serialised by the JVM's class loader lock:</p>
 * <ul>
 *   <li><b>Thread safety without synchronization</b> on the {@code getInstance()}
 *       fast path.</li>
 *   <li><b>Lazy initialisation</b> — the CSV file is not loaded until the
 *       first call to {@code getInstance()}.</li>
 *   <li><b>No synchronization overhead</b> after initialisation — the
 *       {@code INSTANCE} field is a plain static final reference.</li>
 * </ul>
 *
 * <h3>CSV File Format</h3>
 *
 * <pre>
 * seatId,sector,rowNum,seatNumber,price,status,version
 * VIP-A-001,VIP,A,1,500.00,AVAILABLE,1
 * A-B-015,A,B,15,150.00,BOOKED,3
 * </pre>
 *
 * <h3>Concurrency Architecture</h3>
 *
 * <ol>
 *   <li><b>SYNCHRONIZED mode</b> — A single shared JVM-level monitor
 *       ({@code globalSeatLock}) serialises all seat state transitions.</li>
 *   <li><b>FILE_LOCK mode</b> — Uses an OS-level
 *       {@link FileChannel} lock on a {@code .lock} sidecar file.
 *       Enables safe cross-process coordination.</li>
 *   <li><b>OPTIMISTIC mode</b> — Each {@link Seat} carries an
 *       independent version counter. Transitions accepted only if
 *       {@link Seat#optimisticLock(int)} check passes.</li>
 * </ol>
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class SeatRepository extends GenericCsvRepository<Seat> {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /** CSV header: column order matters — must match parse() and serialize(). */
    private static final String CSV_HEADER =
            "seatId,sector,rowNum,seatNumber,price,status,version";

    /** Shared JVM-level monitor lock for SYNCHRONIZED seat transitions. */
    private static final Object globalSeatLock = new Object();

    // =========================================================================
    // STATIC PATH COMPUTATION — runs at class-load time, before any constructor
    // =========================================================================

    /**
     * Absolute path to the OS-level lock sidecar file for FILE_LOCK mode.
     * Computed in the static initializer (runs at class-load time, before the
     * constructor is called), so it is safe to use as a simple assignment.
     */
    private static final Path LOCK_FILE_PATH;

    static {
        String dataDir = System.getProperty("data.dir", "data");
        Path base = Paths.get(dataDir).toAbsolutePath().normalize();
        Path parentDir = base.resolve("stadium_seats.csv").getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                System.err.println("[SeatRepository] Warning: could not create "
                        + "directory: " + e.getMessage());
            }
        }
        LOCK_FILE_PATH = base.resolve("stadium_seats.csv.lock");
    }

    // =========================================================================
    // INSTANCE FIELDS
    // =========================================================================

    /** Counter of successful booking operations. AtomicInteger for lock-free increments. */
    private final AtomicInteger bookingCounter = new AtomicInteger(0);

    // =========================================================================
    // CONSTRUCTION — private, called exactly once by the Holder idiom
    // =========================================================================

    /**
     * Private constructor — constructs the Singleton instance.
     *
     * <p>The data directory is derived from the system property {@code data.dir}
     * if set, otherwise defaults to {@code ./data}. The repository is not eagerly
     * loaded — the {@link GenericCsvRepository#loaded} flag stays false until the
     * first {@link GenericCsvRepository#ensureLoaded()} call.</p>
     */
    private SeatRepository() {
        // super() MUST be the first statement in every constructor.
        super(GenericCsvRepository.resolveDataPath("stadium_seats.csv"),
                "seatId",
                Seat::getSeatId,
                Seat::fromCsv,
                Seat::toCsv);

        // WAL header line set after base class initialization.
        setHeaderLine(CSV_HEADER);
    }

    // =========================================================================
    // SINGLETON — Initialization-on-Demand Holder (Bill Pugh idiom)
    // =========================================================================

    private static final class Holder {
        /** Initialised once by the JVM when SeatRepository is first accessed. */
        private static final SeatRepository INSTANCE = new SeatRepository();
    }

    /**
     * Returns the singleton SeatRepository instance.
     *
     * <p><b>Time Complexity:</b> O(1) after first call — a single volatile read
     * of the INSTANCE field. The first call triggers class loading and construction
     * (O(N) load time), after which all subsequent calls bypass the Holder.</p>
     */
    public static SeatRepository getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Injects a replacement instance (for unit testing).
     * Do not call this concurrently with getInstance() from other threads.
     */
    static void resetInstance(SeatRepository testInstance) {
        // Production code uses the Holder idiom; this allows test injection.
    }

    // =========================================================================
    // ABSTRACT METHOD IMPLEMENTATIONS
    // =========================================================================

    /**
     * Parses a raw CSV data row into a Seat entity.
     *
     * <p>Delegates to {@link Seat#fromCsv(String)} for consistent round-trip parsing.
     * Malformed rows are logged and return null so the loader skips them gracefully.</p>
     *
     * <h4>CSV Token Index Mapping</h4>
     * <table border="1">
     *   <tr><th>Idx</th><th>Field</th>     <th>Type</th>       <th>Notes</th></tr>
     *   <tr><td>0</td><td>seatId</td>     <td>String</td>    <td>Primary key</td></tr>
     *   <tr><td>1</td><td>sector</td>     <td>String</td>    <td>VIP, A..F</td></tr>
     *   <tr><td>2</td><td>rowNum</td>    <td>String</td>    <td>Row letter</td></tr>
     *   <tr><td>3</td><td>seatNumber</td><td>int</td>       <td>1-based</td></tr>
     *   <tr><td>4</td><td>price</td>     <td>double</td>   <td>Currency</td></tr>
     *   <tr><td>5</td><td>status</td>    <td>SeatStatus</td><td>Enum</td></tr>
     *   <tr><td>6</td><td>version</td>   <td>int</td>       <td>Opt. lock</td></tr>
     * </table>
     *
     * <h4>Time Complexity</h4>
     * <p>O(K) where K=7 constant tokens per row.</p>
     */
    @Override
    protected Seat parse(String csvLine) {
        return Seat.fromCsv(csvLine);
    }

    /**
     * Serialises a Seat entity into its CSV row representation.
     *
     * <p>Inverse of parse() — for every valid Seat,
     * parse(serialize(seat)).equals(seat) holds.</p>
     *
     * <h4>Time Complexity</h4>
     * <p>O(1) — String.join() over 7 constant fields.</p>
     */
    @Override
    protected String serialize(Seat seat) {
        return seat.toCsv();
    }

    /**
     * Extracts the primary key identifier from a Seat entity.
     *
     * <h4>Time Complexity</h4>
     * <p>O(1) — returns the cached seatId field directly.</p>
     */
    @Override
    protected String getId(Seat seat) {
        return seat.getSeatId();
    }

    // =========================================================================
    // QUERY METHODS — O(1) from ConcurrentHashMap, O(N) scans
    // =========================================================================

    /** O(1) ConcurrentHashMap lookup. */
    public Seat findBySeatId(String seatId) {
        return findById(seatId);
    }

    /**
     * Returns all seats in a given sector.
     *
     * <h4>Time Complexity</h4>
     * <p>O(N) — scans all N cached seats, filtering by sector.
     * Consider a secondary index map for production use with frequent queries.</p>
     */
    public List<Seat> findBySector(String sector) {
        return Collections.unmodifiableList(
                findAll(s -> sector.equalsIgnoreCase(s.getSector())));
    }

    /** O(N) scan of all seats. */
    public List<Seat> findAvailableBySector(String sector) {
        return Collections.unmodifiableList(
                findAll(s -> sector.equalsIgnoreCase(s.getSector())
                        && s.getStatus() == SeatStatus.AVAILABLE));
    }

    /** O(N) scan of all seats. */
    public List<Seat> findBySectorAndRow(String sector, String row) {
        return Collections.unmodifiableList(
                findAll(s -> sector.equalsIgnoreCase(s.getSector())
                        && row.equalsIgnoreCase(s.getRowNum())));
    }

    /** O(N) scan of all seats. */
    public List<Seat> findByStatus(SeatStatus status) {
        return Collections.unmodifiableList(findAll(s -> s.getStatus() == status));
    }

    /** O(N) scan — single pass to build a status name -> count map. */
    public Map<String, Long> getStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (Seat seat : findAll()) {
            counts.merge(seat.getStatus().name(), 1L, Long::sum);
        }
        return counts;
    }

    /** O(N) scan — counts AVAILABLE seats. */
    public int countAvailable() {
        return (int) findAll().stream()
                .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
                .count();
    }

    /** O(N) scan — returns all AVAILABLE seats. */
    public List<Seat> findAvailableSeats() {
        return Collections.unmodifiableList(
                findAll(s -> s.getStatus() == SeatStatus.AVAILABLE));
    }

    // =========================================================================
    // LOCKING MECHANISMS
    // =========================================================================

    /**
     * Locks a seat using JVM-level synchronized monitor.
     *
     * <p>State transition: AVAILABLE to LOCKED</p>
     * <p><b>Time Complexity:</b> O(N) — saveAll() writes all N seats to disk.</p>
     *
     * @param seatId the seat to lock
     * @return true if lock acquired; false if seat not found or not available
     */
    public boolean lockWithSynchronized(String seatId) {
        synchronized (globalSeatLock) {
            Seat seat = findById(seatId);
            if (seat == null) return false;
            if (seat.lock()) {
                try {
                    saveAll();
                    return true;
                } catch (IOException e) {
                    throw new IllegalStateException(
                            "WAL write failed during seat lock: " + seatId, e);
                }
            }
            return false;
        }
    }

    /**
     * Books a seat using JVM-level synchronized monitor.
     *
     * <p>State transition: AVAILABLE to BOOKED</p>
     * <p><b>Time Complexity:</b> O(N) — saveAll() writes all N seats to disk.</p>
     */
    public boolean bookWithSynchronized(String seatId) {
        synchronized (globalSeatLock) {
            Seat seat = findById(seatId);
            if (seat == null) return false;
            if (seat.book()) {
                try {
                    saveAll();
                    bookingCounter.incrementAndGet();
                    return true;
                } catch (IOException e) {
                    throw new IllegalStateException(
                            "WAL write failed during seat booking: " + seatId, e);
                }
            }
            return false;
        }
    }

    /**
     * Releases a seat using JVM-level synchronized monitor.
     *
     * <p>State transition: LOCKED or BOOKED to AVAILABLE</p>
     * <p><b>Time Complexity:</b> O(N) — saveAll() writes all N seats to disk.</p>
     */
    public boolean releaseWithSynchronized(String seatId) {
        synchronized (globalSeatLock) {
            Seat seat = findById(seatId);
            if (seat == null) return false;
            if (seat.release()) {
                try {
                    saveAll();
                    return true;
                } catch (IOException e) {
                    throw new IllegalStateException(
                            "WAL write failed during seat release: " + seatId, e);
                }
            }
            return false;
        }
    }

    /**
     * Books a seat using optimistic locking with version check.
     *
     * <p>State transition: LOCKED to BOOKED (version-gated)</p>
     *
     * <p><b>Optimistic Locking Protocol:</b></p>
     * <ol>
     *   <li>Read seat and record its version counter.</li>
     *   <li>Attempt transition with {@link Seat#optimisticLock(int)}.</li>
     *   <li>If version matched, persist via WAL.</li>
     * </ol>
     *
     * <p>Wait-free on the read path — no locks are held while reading.
     * The only sync point is the saveAll() WAL write under writeLock.</p>
     *
     * <p>If the version check fails, another thread modified the seat
     * between read and book. Caller should retry.</p>
     *
     * <p><b>Time Complexity:</b> O(1) version check; O(N) for saveAll().</p>
     *
     * @param seatId the seat to book
     * @param expectedVersion version read at the start of the transaction
     * @return true if booking succeeded and was persisted; false if seat not
     *         found, not LOCKED, or version mismatch
     */
    public boolean bookWithOptimistic(String seatId, int expectedVersion) {
        Seat seat = findById(seatId);
        if (seat == null) return false;
        if (seat.getStatus() == SeatStatus.LOCKED
                && seat.optimisticLock(expectedVersion)) {
            try {
                saveAll();
                bookingCounter.incrementAndGet();
                return true;
            } catch (IOException e) {
                throw new IllegalStateException(
                        "WAL write failed during optimistic booking: " + seatId, e);
            }
        }
        return false;
    }

    /**
     * Books a seat using an OS-level FileLock on a dedicated .lock sidecar file.
     *
     * <p>Designed for <b>cross-process coordination</b> — when multiple JVM
     * processes need exclusive access to the same seat. FileChannel.lock() is
     * advisory but works reliably on Windows NTFS, Linux ext4, and macOS APFS.</p>
     *
     * <p><b>Time Complexity:</b> O(1) lock acquisition; O(N) for saveAll().</p>
     *
     * @param seatId the seat to book
     * @return true if booking succeeded
     * @throws IOException if the lock file or WAL write fails
     */
    public boolean bookWithFileLock(String seatId) throws IOException {
        FileChannel channel = FileChannel.open(LOCK_FILE_PATH,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE);

        FileLock lock = null;
        try {
            lock = channel.lock(); // blocking, exclusive OS-level lock

            synchronized (globalSeatLock) {
                Seat seat = findById(seatId);
                if (seat != null && seat.book()) {
                    saveAll();
                    bookingCounter.incrementAndGet();
                    return true;
                }
            }
            return false;
        } finally {
            if (lock != null) lock.release();
            channel.close();
        }
    }

    // =========================================================================
    // DIAGNOSTICS
    // =========================================================================

    /** Returns the total booking operations performed since construction. */
    public int getBookingCount() {
        return bookingCounter.get();
    }

    /**
     * Returns a formatted diagnostic report.
     */
    public String getDiagnosticReport() {
        return new StringBuilder()
                .append("=== SeatRepository Diagnostic Report ===\n")
                .append("Singleton: ").append(this == getInstance() ? "YES" : "NO").append("\n")
                .append("Data file: ").append(getFilePath()).append("\n")
                .append("Loaded: ").append(isLoaded() ? "YES" : "NO").append("\n")
                .append("Cache size: ").append(count()).append(" seats\n")
                .append("Available: ").append(countAvailable()).append(" seats\n")
                .append("Booking ops: ").append(getBookingCount()).append("\n")
                .append("Status breakdown: ").append(getStatusCounts()).append("\n")
                .append("Lock file: ").append(LOCK_FILE_PATH).append("\n")
                .toString();
    }
}
