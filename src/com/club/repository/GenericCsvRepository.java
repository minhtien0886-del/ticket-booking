package com.club.repository;

import com.club.util.WriteAheadLog;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public abstract class GenericCsvRepository<T> {
    protected final Path filePath;
    protected final ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>();
    protected static final String CSV_DELIMITER = ",";

    /** Human-readable name of the primary key column, used in diagnostics. */
    protected final String idFieldName;

    /**
     * Intrinsic lock serialising all write operations on the WAL pipeline.
     * Distinct from loadLock to keep reads lock-free.
     */
    protected final Object writeLock = new Object();

    /** Header line for the CSV file, written as the first line of every WAL write. */
    protected String headerLine;

    /**
     * Volatile sentinel tracking whether the cache has been loaded.
     * Once true, all reads are O(1) without any locking.
     *
     * <p>Why volatile is sufficient: the JMM guarantees that all writes by the
     * thread that sets loaded=true (inside synchronized(loadLock)) are visible
     * to any thread that subsequently reads loaded==true. This is the classic
     * double-checked locking guarantee.</p>
     */
    private volatile boolean loaded = false;

    /** Count of records loaded during the last loadAll() call. */
    private volatile int loadedCount = 0;

    /**
     * Private lock for the double-checked locking pattern in ensureLoaded().
     * Distinct from writeLock because only one thread should ever load.
     */
    private final Object loadLock = new Object();

    // Functional conversion callbacks — stored as final fields at class level
    // so the constructor can assign to them and subclasses' override methods use them.
    private final Function<T, String> idExtractor;
    private final Function<String, T> csvParser;
    private final Function<T, String> csvSerializer;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================

    /**
     * Constructs a new GenericCsvRepository with the given CSV file path and
     * functional callbacks for entity-to/from-CSV conversion.
     *
     * @param filePath      absolute path to the CSV file
     * @param idFieldName   human-readable name of the primary key column
     * @param idExtractor   extracts the string ID from an entity
     * @param csvParser     parses a raw CSV row into an entity instance
     * @param csvSerializer serialises an entity to its CSV row representation
     */
    public GenericCsvRepository(Path filePath, String idFieldName,
                               Function<T, String> idExtractor,
                               Function<String, T> csvParser,
                               Function<T, String> csvSerializer) {
        this.filePath       = filePath;
        this.idFieldName    = idFieldName;
        this.idExtractor    = idExtractor;
        this.csvParser      = csvParser;
        this.csvSerializer  = csvSerializer;
    }

    /**
     * Protected helper that subclasses can call before their {@code super()} statement
     * (from a static context or from within a {@code static} initializer block) to
     * compute the absolute path to a data file and ensure its parent directory exists.
     *
     * <p>Example usage in a Singleton constructor:</p>
     * <pre>
     * private SeatRepository() {
     *     Path csvPath = resolveDataPath("stadium_seats.csv");
     *     super(csvPath, "seatId", Seat::getSeatId, Seat::fromCsv, Seat::toCsv);
     * }
     * </pre>
     *
     * @param relativePath a path relative to the data directory (e.g., "seats.csv")
     * @return the resolved absolute Path
     */
    protected static Path resolveDataPath(String relativePath) {
        String dataDir = System.getProperty("data.dir", "data");
        Path base = Paths.get(dataDir).toAbsolutePath().normalize();
        Path target = base.resolve(relativePath);
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                System.err.println("[GenericCsvRepository] Warning: could not create "
                        + "directory " + parent + ": " + e.getMessage());
            }
        }
        return target;
    }

    // =========================================================================
    // SETTER (used by subclasses after super() call)
    // =========================================================================

    /**
     * Sets the CSV header line used for WAL writes.
     *
     * @param header the header row string, e.g. "id,name,email"
     */
    protected void setHeaderLine(String header) {
        this.headerLine = header;
    }

    // =========================================================================
    // CORE LOADING — Double-Checked Locking
    // =========================================================================

    /**
     * Ensures the cache is populated from disk using double-checked locking.
     *
     * <p><b>Protocol:</b></p>
     * <ol>
     *   <li>First check (no lock): if loaded==true, return immediately. O(1) fast path.</li>
     *   <li>Acquire loadLock synchronisation.</li>
     *   <li>Second check (inside synchronized): another thread may have loaded already.</li>
     *   <li>Execute loadAll() — exactly one thread does this work.</li>
     *   <li>Set loaded=true — subsequent threads bypass the lock.</li>
     * </ol>
     *
     * <p><b>JMM guarantee:</b> The volatile write to loaded=true inside
     * synchronized(loadLock) makes all preceding writes (cache population,
     * loadedCount) visible to any thread that reads loaded==true.</p>
     *
     * <h4>Time Complexity</h4>
     * Best: O(1) (already loaded). Worst: O(N) (first load of N entities).
     *
     * @throws IOException if the CSV file cannot be read
     */
    public void ensureLoaded() throws IOException {
        if (!loaded) {                                    // ① fast path — no lock
            synchronized (loadLock) {                      // ② serialised entry
                if (!loaded) {                             // ③ re-check after acquire
                    loadAll();
                    loaded = true;
                }
            }
        }
    }

    /**
     * Loads all records from the CSV file into the cache.
     *
     * <p><b>Pipeline:</b></p>
     * <ol>
     *   <li>Create the file + parent directory if absent.</li>
     *   <li>Call WAL.needsRecovery() — if a .tmp exists, recover from .bak.</li>
     *   <li>Parse CSV with 64KB BufferedReader — one pass over N rows.</li>
     *   <li>Populate ConcurrentHashMap — each put() is atomic and lock-free.</li>
     * </ol>
     *
     * <h4>Time Complexity</h4>
     * <p>O(N) — single-pass parse over all N CSV rows. Each row is read once
     * and parsed once.</p>
     *
     * <h4>Space Complexity</h4>
     * <p>O(N) — ConcurrentHashMap stores all N entities in memory.</p>
     */
    protected void loadAll() throws IOException {
        if (!Files.exists(filePath)) {
            Path parent = filePath.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.createFile(filePath);
            cache.clear();
            loadedCount = 0;
            return;
        }

        // WAL recovery — restore from backup if a prior write was interrupted.
        if (WriteAheadLog.needsRecovery(filePath)) {
            System.err.println("[WAL] Detected interrupted write on "
                    + filePath.getFileName() + " — initiating recovery...");
            WriteAheadLog.recover(filePath);
            System.err.println("[WAL] Recovery complete.");
        }

        cache.clear();
        int count = 0;
        long startNs = System.nanoTime();

        // 64KB BufferedReader for optimal I/O throughput on large CSV files.
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(filePath.toFile()), StandardCharsets.UTF_8),
                64 * 1024)) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (isFirstLine) {
                    isFirstLine = false;
                    if (headerLine == null || headerLine.isEmpty()) {
                        headerLine = line;
                    }
                    continue;
                }

                try {
                    T entity = csvParser.apply(line);
                    if (entity != null) {
                        String id = idExtractor.apply(entity);
                        if (id != null) {
                            cache.put(id, entity);
                            count++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[GenericCsvRepository] Skipping corrupt row: "
                            + e.getMessage());
                }
            }
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        this.loadedCount = count;
        System.err.println(String.format("[%s] Loaded %,d records in %dms",
                getClass().getSimpleName(), count, elapsedMs));
    }

    // =========================================================================
    // PERSISTENCE — WAL-Integrated Save Pipeline
    // =========================================================================

    /**
     * Persists the entire cache to CSV using the Write-Ahead Logging engine.
     *
     * <p><b>WAL Pipeline:</b></p>
     * <ol>
     *   <li>Serialise all cached entities to CSV rows.</li>
     *   <li>WriteAheadLog.writeWithWal():</li>
     *   <ol type="a">
     *     <li>Acquire OS-level FileLock on .lock sidecar.</li>
     *     <li>Write all rows to .tmp via 64KB BufferedWriter.</li>
     *     <li>Copy main file to .bak as safety backup.</li>
     *     <li>ATOMIC_MOVE .tmp → main file.</li>
     *     <li>Delete .bak and release FileLock.</li>
     *   </ol>
     * </ol>
     *
     * <p><b>Crash-safety:</b> After flush() returns, either the new or previous
     * state is recoverable. No partial writes are possible.</p>
     *
     * <h4>Time Complexity</h4>
     * O(N) — serialise all N entities + WAL write all N rows. ATOMIC_MOVE is O(1).</p>
     */
    public void saveAll() throws IOException {
        synchronized (writeLock) {
            List<String> rows = new ArrayList<>(cache.size());
            for (T entity : cache.values()) {
                rows.add(csvSerializer.apply(entity));
            }
            try {
                WriteAheadLog.writeWithWal(filePath, headerLine, rows);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("WAL write interrupted", e);
            }
        }
    }

    // =========================================================================
    // CRUD — READ OPERATIONS (lock-free O(1) from ConcurrentHashMap)
    // =========================================================================

    /**
     * Retrieves a single entity by its unique identifier.
     *
     * <h4>Time Complexity</h4>
     * <p>O(1) guaranteed — ConcurrentHashMap.get() uses a lock-free striped
     * hash table. Even in the pathological worst case (adversarial hash),
     * JDK falls back to a balanced tree, keeping worst-case at O(log N).</p>
     *
     * @param id the unique identifier; must not be null
     * @return the entity, or null if not found or cache not yet loaded
     */
    public T findById(String id) {
        ensureLoadedQuietly();
        return cache.get(id);
    }

    /**
     * Returns a snapshot List of all entities in the cache.
     *
     * <p>Returns a new ArrayList snapshot — changes to the cache after this call
     * do not affect the returned list, and vice versa.</p>
     *
     * <h4>Time Complexity</h4>
     * <p>O(N) — iterates over all N cached entries to copy them into result list.</p>
     *
     * @return a new independent List; never null (empty list if cache is empty)
     */
    public List<T> findAll() {
        ensureLoadedQuietly();
        return new ArrayList<>(cache.values());
    }

    /**
     * Returns all entities satisfying the given predicate.
     *
     * <p><b>No short-circuit:</b> Tests all N entities. Use findOne() if only
     * the first match is needed.</p>
     *
     * <h4>Time Complexity</h4>
     * <p>O(N) — evaluates predicate against all N cached entities.</p>
     *
     * @param predicate the filter condition; must not be null
     * @return a new List of matching entities; never null
     */
    public List<T> findAll(Predicate<T> predicate) {
        ensureLoadedQuietly();
        List<T> results = new ArrayList<>();
        for (T entity : cache.values()) {
            if (predicate.test(entity)) {
                results.add(entity);
            }
        }
        return results;
    }

    /**
     * Returns the first entity satisfying the predicate, or null if none match.
     *
     * <p><b>Short-circuit:</b> Stops at the first match, more efficient than
     * findAll(Predicate) when only one result is needed.</p>
     *
     * <h4>Time Complexity</h4>
     * <p>O(K) where K is the index of the first match. Worst case O(N) (no match).</p>
     */
    public T findOne(Predicate<T> predicate) {
        ensureLoadedQuietly();
        for (T entity : cache.values()) {
            if (predicate.test(entity)) {
                return entity;
            }
        }
        return null;
    }

    // =========================================================================
    // CRUD — WRITE OPERATIONS
    // =========================================================================

    /**
     * Inserts or updates an entity in the cache and persists via WAL.
     *
     * <p><b>Pipeline:</b> cache.put() [O(1)] → saveAll() [O(N) WAL write].</p>
     *
     * <p><b>Batch optimisation:</b> Use cacheOnly() for each entity then
     * flush() once. This reduces N disk writes to a single O(N) WAL write.</p>
     *
     * <h4>Time Complexity</h4>
     * <p>O(N) — O(1) cache update + O(N) WAL write of all N entities.</p>
     *
     * @param entity the entity to save; null is silently ignored
     * @throws IOException if the WAL write fails
     */
    public void save(T entity) throws IOException {
        if (entity == null) return;
        String id = idExtractor.apply(entity);
        if (id == null) return;

        synchronized (writeLock) {
            cache.put(id, entity);
            saveAll();
        }
    }

    /**
     * Inserts or updates an entity in the cache WITHOUT writing to disk.
     *
     * <p>Intended for batch write optimisation:</p>
     * <pre>
     * for (Ticket t : batch) {
     *     repo.cacheOnly(t);  // O(1) each, no I/O
     * }
     * repo.flush();             // O(N) WAL write — single disk sync
     * </pre>
     *
     * <h4>Time Complexity</h4>
     * <p>O(1) — ConcurrentHashMap.put() is atomic and lock-free.</p>
     *
     * @param entity the entity to add to cache
     */
    public void cacheOnly(T entity) {
        if (entity == null) return;
        String id = idExtractor.apply(entity);
        if (id != null) {
            cache.put(id, entity);
        }
    }

    /**
     * Persists the current cache state to disk via WAL.
     * Call this after a batch of cacheOnly() operations.
     *
     * <h4>Time Complexity</h4>
     * <p>O(N) — writes all N cached entities to disk.</p>
     *
     * @throws IOException if the WAL operation fails
     */
    public void flush() throws IOException {
        synchronized (writeLock) {
            saveAll();
        }
    }

    /**
     * Removes an entity from the cache and persists the updated state.
     *
     * <p>Silently returns if the ID is not present in the cache.</p>
     *
     * <h4>Time Complexity</h4>
     * <p>O(N) — ConcurrentHashMap.remove() is O(1) + saveAll() writes N-1 rows.</p>
     *
     * @param id the ID of the entity to delete
     * @throws IOException if the WAL write fails
     */
    public void deleteById(String id) throws IOException {
        synchronized (writeLock) {
            if (cache.remove(id) != null) {
                saveAll();
            }
        }
    }

    // =========================================================================
    // QUERY HELPERS
    // =========================================================================

    /**
     * Checks whether an entity with the given ID is in the cache.
     *
     * <h4>Time Complexity</h4>
     * <p>O(1) — ConcurrentHashMap.containsKey() is lock-free.</p>
     */
    public boolean existsById(String id) {
        ensureLoadedQuietly();
        return cache.containsKey(id);
    }

    /**
     * Checks whether any entity satisfies the predicate.
     * Short-circuits on first match.
     *
     * <h4>Time Complexity</h4>
     * <p>O(K) where K is the index of the first match. Worst case O(N).</p>
     */
    public boolean exists(Predicate<T> predicate) {
        ensureLoadedQuietly();
        for (T entity : cache.values()) {
            if (predicate.test(entity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the total number of cached entities.
     *
     * <h4>Time Complexity</h4>
     * <p>O(1) — ConcurrentHashMap.size() returns the cached size field.</p>
     */
    public int count() {
        ensureLoadedQuietly();
        return cache.size();
    }

    /**
     * Returns all entity IDs currently in the cache.
     *
     * <h4>Time Complexity</h4>
     * <p>O(N) — copies all N keys into a new HashSet.</p>
     */
    public Set<String> findAllIds() {
        ensureLoadedQuietly();
        return new HashSet<>(cache.keySet());
    }

    /**
     * Clears the cache and reloads from disk.
     *
     * <h4>Time Complexity</h4>
     * <p>O(N) — equivalent to loadAll().</p>
     */
    public void clearAndReload() throws IOException {
        synchronized (loadLock) {
            loaded = false;
            loadAll();
            loaded = true;
        }
    }

    /**
     * Clears the cache and reloads from disk, silently swallowing any IOException.
     *
     * <p>Convenience method for callers that cannot handle checked exceptions
     * (e.g., repository singletons updated during application startup).</p>
     *
     * @see #clearAndReload()
     */
    public void reload() {
        try {
            clearAndReload();
        } catch (IOException e) {
            System.err.println("[" + getClass().getSimpleName()
                    + "] reload: " + e.getMessage());
        }
    }

    /**
     * Wraps ensureLoaded() and silently swallows any IOException.
     *
     * <p>Use in read operations where a partial read is preferable to
     * propagating an IOException. The cache retains its prior state.</p>
     */
    public void ensureLoadedQuietly() {
        try {
            ensureLoaded();
        } catch (IOException e) {
            System.err.println("[" + getClass().getSimpleName()
                    + "] ensureLoadedQuietly: " + e.getMessage());
        }
    }

    // =========================================================================
    // ABSTRACT METHODS — must be implemented by concrete subclasses
    // =========================================================================

    /**
     * Parses a raw CSV data row (excluding the header) into an entity.
     *
     * <p>Default implementation delegates to the csvParser function passed
     * in the constructor. Override to add validation or custom parsing.</p>
     *
     * @param csvLine a raw CSV row string
     * @return the parsed entity, or null to skip this row
     */
    protected T parse(String csvLine) {
        return csvParser.apply(csvLine);
    }

    /**
     * Serialises an entity into its CSV row representation.
     *
     * <p>Default implementation delegates to the csvSerializer function.
     * Override to add custom formatting or validation.</p>
     *
     * <p><b>Important:</b> Field order must match headerLine exactly.</p>
     *
     * @param entity the entity to serialise
     * @return a CSV row string matching the header format
     */
    protected String serialize(T entity) {
        return csvSerializer.apply(entity);
    }

    /**
     * Extracts the unique identifier string from an entity.
     *
     * <p>Default implementation delegates to the idExtractor function.
     * Override for composite or custom key strategies.</p>
     *
     * @param entity the entity
     * @return the entity's unique string ID, or null to skip
     */
    protected String getId(T entity) {
        return idExtractor.apply(entity);
    }

    // =========================================================================
    // UTILITY & DIAGNOSTICS
    // =========================================================================

    /**
     * Returns the number of records loaded during the last loadAll() call.
     * Snapshot value — not updated by save()/deleteById() unless reload occurs.
     */
    public int getLoadedCount() {
        return loadedCount;
    }

    /** Returns the absolute path to the CSV file managed by this repository. */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * Reports whether loadAll() has completed at least once.
     *
     * <p>Note: Returns true even if all cache entries have been deleted.</p>
     */
    public boolean isLoaded() {
        return loaded;
    }

    /** Returns a human-readable diagnostic summary of this repository's state. */
    public String getCacheStats() {
        return String.format(
                "Repository[%s]: file=%s, loadedCount=%,d, cacheSize=%,d, loaded=%s",
                getClass().getSimpleName(),
                filePath.getFileName(),
                loadedCount,
                cache.size(),
                loaded);
    }
}
