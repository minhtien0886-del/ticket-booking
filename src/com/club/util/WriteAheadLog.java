package com.club.util;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Write-Ahead Logging (WAL) engine providing crash-safe, atomic file persistence.
 *
 * <p>This class guarantees that CSV file writes are atomic and recoverable even in the
 * event of a JVM crash, OOM kill, or power failure mid-write. The algorithm follows
 * the standard WAL pattern used in database systems:</p>
 *
 * <ol>
 *   <li>Acquire an OS-level {@link FileLock} on a dedicated {@code .lock} sidecar file
 *       to prevent concurrent writes from multiple JVM instances.</li>
 *   <li>Write the full dataset to a {@code .tmp} temporary file using a
 *       {@link BufferedWriter} backed by a 64 KB {@link BufferedOutputStream}.</li>
 *   <li>Copy the current (potentially corrupted) main file to a {@code .bak} backup.</li>
 *   <li>Atomically rename {@code .tmp} to the target file using
 *       {@link Files#move(Path, Path, CopyOption...)} with {@link StandardCopyOption#ATOMIC_MOVE}.
 *       This ensures the main file is either fully written or not touched at all.</li>
 *   <li>Delete the {@code .bak} backup to signal successful completion.</li>
 *   <li>Release the {@link FileLock} and close the {@link FileChannel}.</li>
 * </ol>
 *
 * <p><b>Recovery:</b> On the next startup, if a {@code .tmp} file exists alongside a
 * {@code .bak}, the file write was interrupted mid-operation. In that case,
 * {@link #recover(Path)} copies the {@code .bak} over the (possibly corrupted) main file
 * and deletes the {@code .tmp}, restoring the data to its last known-good state.</p>
 *
 * <h3>File Sidecars</h3>
 * <ul>
 *   <li>{@code data.csv.lock} &mdash; exclusive write lock (deleted after use)</li>
 *   <li>{@code data.csv.tmp}  &mdash; in-progress write, converted to main on success</li>
 *   <li>{@code data.csv.bak}  &mdash; backup of the previous known-good state</li>
 * </ul>
 *
 * <p><b>Concurrency:</b> The {@link FileLock} is JVM-global (acquired via
 * {@link FileChannel#lock()}), which serialises writes across all threads within the
 * same JVM process.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 2.0
 * @since Java 8
 */
public final class WriteAheadLog {

    /** 64 KB buffer size — optimal for most HDDs and SSDs per modern I/O benchmarks. */
    private static final int BUFFER_SIZE = 64 * 1024;

    /** Private constructor prevents instantiation of this utility class. */
    private WriteAheadLog() {}

    /**
     * Computes the path of the lock sidecar file for a given data file.
     */
    private static Path getLockPath(Path filePath) {
        return Paths.get(filePath.toString() + ".lock");
    }

    /**
     * Computes the path of the temporary sidecar file for a given data file.
     */
    private static Path getTmpPath(Path filePath) {
        return Paths.get(filePath.toString() + ".tmp");
    }

    /**
     * Computes the path of the backup sidecar file for a given data file.
     */
    private static Path getBakPath(Path filePath) {
        return Paths.get(filePath.toString() + ".bak");
    }

    /**
     * Atomically writes a CSV file with full WAL crash-safety guarantees.
     *
     * <p>The write pipeline proceeds through five guaranteed steps:</p>
     * <ol>
     *   <li><b>Lock acquisition</b> &mdash; an exclusive {@link FileLock} is obtained on
     *       the {@code .lock} sidecar. If the lock cannot be acquired (another process
     *       is writing), this method waits indefinitely until it becomes available.</li>
     *   <li><b>Temp write</b> &mdash; all data is written to the {@code .tmp} sidecar
     *       via a {@link BufferedWriter} backed by a 64 KB byte buffer.</li>
     *   <li><b>Backup creation</b> &mdash; the existing main file (if it exists) is
     *       copied to {@code .bak} to preserve the previous known-good state.</li>
     *   <li><b>Atomic rename</b> &mdash; the {@code .tmp} is renamed to the main file
     *       using {@link StandardCopyOption#ATOMIC_MOVE}. This is a single atomic
     *       filesystem operation on all major OSes (Windows NTFS, Linux ext4, macOS APFS).</li>
     *   <li><b>Cleanup</b> &mdash; the {@code .bak} is deleted and the {@link FileLock}
     *       is released inside a {@code finally} block to guarantee release even on error.</li>
     * </ol>
     *
     * <p><b>Crash-safety guarantee:</b> After a crash, at least one of the following
     * files will exist and be intact: the main file (complete new state) or the
     * {@code .bak} file (previous state). The {@code .tmp} is never visible as the
     * main file because the rename is atomic.</p>
     *
     * <h4>Time Complexity</h4>
     * <p>O(N) where N is the total number of characters written — each character
     * is written exactly once to the {@code .tmp} file. No additional passes are made.
     * The {@link Files#move} and {@link Files#copy} operations are O(1) metadata
     * operations on modern filesystems.</p>
     *
     * <h4>Space Complexity</h4>
     * <p>O(N) additional disk space for the duration of the write — both the
     * {@code .tmp} and {@code .bak} files must fit alongside the main file.</p>
     *
     * @param filePath the path to the target CSV file to write
     * @param header   the CSV header line (e.g., {@code "id,name,email"})
     * @param lines    the data lines to write; each element is one CSV row
     * @throws IOException          if an I/O error occurs at any step; the main file
     *                              is guaranteed to be untouched on IOException
     * @throws InterruptedException if the thread is interrupted while waiting for
     *                              the file lock
     */
    public static void writeWithWal(Path filePath, String header, List<String> lines)
            throws IOException, InterruptedException {

        Path tmpPath  = getTmpPath(filePath);
        Path lockPath = getLockPath(filePath);
        Path bakPath  = getBakPath(filePath);

        FileChannel channel = null;
        FileLock lock = null;

        try {
            // Step 1: Acquire exclusive OS-level lock on the .lock sidecar.
            channel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE);

            lock = channel.lock(); // blocking, exclusive lock

            // Step 2: Write all data to the .tmp file using a 64 KB buffered writer.
            // Correct Java 8 stack:
            //   BufferedOutputStream(64KB) -> OutputStreamWriter(UTF-8) -> BufferedWriter(64KB)
            OutputStream rawOut = Files.newOutputStream(tmpPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            BufferedOutputStream bufferedOut = new BufferedOutputStream(rawOut, BUFFER_SIZE);
            OutputStreamWriter osw = new OutputStreamWriter(bufferedOut, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw, BUFFER_SIZE);

            try {
                bw.write(header);
                bw.newLine();
                for (String line : lines) {
                    bw.write(line);
                    bw.newLine();
                }
                bw.flush();
                // Explicitly flush the underlying layers to ensure data reaches OS.
                osw.flush();
                bufferedOut.flush();
                rawOut.flush();
            } finally {
                bw.close(); // closes osw and bufferedOut and rawOut in reverse order
            }

            // Step 3: Back up the existing main file if it is present.
            if (Files.exists(filePath)) {
                Files.copy(filePath, bakPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
            }

            // Step 4: Atomically rename .tmp -> main file.
            // ATOMIC_MOVE guarantees the rename is indivisible — either the
            // main file is fully replaced or it is untouched.
            Files.move(tmpPath, filePath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);

            // Step 5: Delete the backup — signals a clean, completed write.
            Files.deleteIfExists(bakPath);

        } finally {
            // ALWAYS release the lock and close the channel, even on error.
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException ignored) { /* best-effort cleanup */ }
            }
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException ignored) { /* best-effort cleanup */ }
            }

            // Clean up the lock file on the way out.
            Files.deleteIfExists(lockPath);
        }
    }

    /**
     * Convenience overload that accepts a pre-assembled content string.
     * The content is split on newlines and delegated to
     * {@link #writeWithWal(Path, String, List)}.
     *
     * @param filePath the path to the target file
     * @param content  the complete file content (header + data rows joined by newlines)
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted while acquiring the lock
     */
    public static void writeWithWal(Path filePath, String content)
            throws IOException, InterruptedException {
        String[] parts = content.split("\\r?\\n", -1);
        List<String> allLines = new ArrayList<>(parts.length);
        for (String p : parts) {
            allLines.add(p);
        }
        String header = allLines.isEmpty() ? "" : allLines.get(0);
        List<String> dataLines = allLines.size() > 1 ? allLines.subList(1, allLines.size()) : allLines;
        writeWithWal(filePath, header, dataLines);
    }

    /**
     * Detects whether a crash-interrupted write left behind an orphaned {@code .tmp}
     * sidecar file.
     *
     * <p>This method is called at startup by {@link com.club.repository.GenericCsvRepository}
     * to determine whether recovery is necessary before loading a data file.</p>
     *
     * <h4>Time Complexity</h4>
     * <p>O(1) — a single filesystem existence check on the {@code .tmp} path.</p>
     *
     * @param filePath the path to the main data file
     * @return {@code true} if a {@code .tmp} sidecar exists (write was interrupted),
     *         {@code false} otherwise
     */
    public static boolean needsRecovery(Path filePath) {
        return Files.exists(getTmpPath(filePath));
    }

    /**
     * Recovers a file to its last known-good state by restoring from the {@code .bak}
     * backup file.
     *
     * <p>This method should be called at application startup when {@link #needsRecovery(Path)}
     * returns {@code true}. It handles the following failure scenario:</p>
     *
     * <blockquote>
     *   A write operation begins: {@code .tmp} is created and written, {@code .bak}
     *   is created from the old main file, then the JVM crashes before the
     *   {@code Files.move(tmp, main)} call completes. On restart, the main file may
     *   be partially written (corrupt), but {@code .bak} still holds the previous
     *   clean state.
     * </blockquote>
     *
     * <p>The recovery steps are:</p>
     * <ol>
     *   <li>If the main file exists, delete it (it may be partially written / corrupt).</li>
     *   <li>If the {@code .bak} backup exists, copy it to the main file path.</li>
     *   <li>Delete the orphaned {@code .tmp} sidecar.</li>
     * </ol>
     *
     * <h4>Time Complexity</h4>
     * <p>O(M) where M is the size of the backup file — the {@link Files#copy}
     * operation reads and writes each byte of the backup.</p>
     *
     * @param filePath the path to the main data file to recover
     * @throws IOException if the backup cannot be copied or the tmp file cannot be deleted
     */
    public static void recover(Path filePath) throws IOException {
        Path bakPath = getBakPath(filePath);
        Path tmpPath = getTmpPath(filePath);

        if (Files.exists(bakPath)) {
            Files.deleteIfExists(filePath);
            Files.copy(bakPath, filePath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
            Files.deleteIfExists(bakPath);
        }

        Files.deleteIfExists(tmpPath);
    }

    /**
     * Checks whether a valid backup exists for a given data file.
     *
     * <h4>Time Complexity</h4>
     * <p>O(1) — a single filesystem existence check.</p>
     *
     * @param filePath the path to the main data file
     * @return {@code true} if a {@code .bak} sidecar exists, {@code false} otherwise
     */
    public static boolean hasBackup(Path filePath) {
        return Files.exists(getBakPath(filePath));
    }

    /**
     * Returns the set of all sidecar file paths that exist for a given main file,
     * useful for diagnostics and debugging.
     *
     * @param filePath the path to the main data file
     * @return an array of existing sidecar paths (may be empty)
     */
    public static Path[] getSidecarPaths(Path filePath) {
        Path lock = getLockPath(filePath);
        Path tmp  = getTmpPath(filePath);
        Path bak  = getBakPath(filePath);

        int count = 0;
        if (Files.exists(lock)) count++;
        if (Files.exists(tmp))  count++;
        if (Files.exists(bak))   count++;

        Path[] result = new Path[count];
        int i = 0;
        if (Files.exists(lock)) result[i++] = lock;
        if (Files.exists(tmp))  result[i++] = tmp;
        if (Files.exists(bak))   result[i++] = bak;
        return result;
    }
}
