
## Table of Contents

1. [Testing Architecture](#1-testing-architecture)
2. [JUnit Test Suite Structure](#2-junit-test-suite-structure)
3. [Concurrency Core Tests](#3-concurrency-core-tests)
4. [Write-Ahead Logging (WAL) Storage Tests](#4-write-ahead-logging-wal-storage-tests)
5. [Data Reset & Environment Setup](#5-data-reset--environment-setup)
6. [Running the Application](#6-running-the-application)

---

## 1. Testing Architecture

The FCM-ERP test strategy is organized into four layers:

| Layer | Scope | Tools | Frequency |
|-------|-------|-------|-----------|
| **Unit Tests** | Entity parsing, state machines, business rules | JUnit 4 custom harness | Per-feature |
| **Integration Tests** | Repository + WAL pipeline, cache invalidation | Custom test runners | Per-sprint |
| **Concurrency Tests** | Multi-threaded booking, lock-mode verification | `TicketSimulator` | Per-release |
| **Acceptance Tests** | End-to-end CLI flows, RBAC, reporting | Manual + test scenarios | Per-release |

> **Note:** The project uses a custom JUnit-style test framework built on Java 8 assertions. No external test libraries are required — all testing infrastructure uses pure `java.lang.AssertionError` and standard assertions.

---

## 2. JUnit Test Suite Structure

### Test Package Layout

```
src/com/club/
├── model/
│   ├── SeatTest.java           # State machine, version counter, CSV round-trip
│   ├── FanTest.java            # Balance, loyalty tier, deposit logic
│   ├── MerchandiseTest.java    # Stock reservation, price with tax
│   └── ...
├── repository/
│   ├── GenericCsvRepositoryTest.java    # Cache load, WAL round-trip, CRUD
│   ├── SeatRepositoryTest.java          # Lock-mode dispatch, findBySector
│   ├── MerchandiseRepositoryTest.java   # Seed, findActive, decrementStock
│   └── ...
├── service/
│   ├── TicketServiceTest.java   # Booking pipeline, rollback, balance check
│   ├── ShoppingCartServiceTest.java
│   └── HumanResourceServiceTest.java
├── security/
│   ├── AuthenticationManagerTest.java  # Password hashing, constant-time compare
│   └── SecurityContextTest.java        # ThreadLocal isolation, permission guards
└── simulator/
    └── TicketSimulatorTest.java      # Concurrent benchmark execution
```

### Core Test Patterns

#### Pattern 1: Entity CSV Round-Trip

```java
public class SeatTest {
    @Test
    public void testFromCsvAndToCsv_areInverse() {
        Seat original = new Seat("VIP-A-001", "VIP", "A", 1, 500.0, SeatStatus.AVAILABLE);
        String csv = original.toCsv();
        Seat parsed = Seat.fromCsv(csv);
        assertEquals(original.getSeatId(), parsed.getSeatId());
        assertEquals(original.getPrice(), parsed.getPrice(), 0.001);
        assertEquals(original.getStatus(), parsed.getStatus());
        assertEquals(original.getVersion(), parsed.getVersion());
    }

    @Test
    public void testOptimisticLock_versionMismatch_returnsFalse() {
        Seat seat = new Seat("A-B-001", "A", "B", 1, 150.0, SeatStatus.AVAILABLE);
        int staleVersion = seat.getVersion(); // 1
        seat.setStatus(SeatStatus.BOOKED);  // increments to 2
        assertFalse(seat.optimisticLock(staleVersion)); // stale: 1 vs current: 2
    }

    @Test
    public void testOptimisticLock_versionMatch_returnsTrue() {
        Seat seat = new Seat("A-B-002", "A", "B", 2, 150.0, SeatStatus.AVAILABLE);
        int currentVersion = seat.getVersion(); // 1
        assertTrue(seat.optimisticLock(currentVersion)); // 1 == 1
        assertEquals(2, seat.getVersion());             // incremented
    }
}
```

#### Pattern 2: Repository Cache + WAL Round-Trip

```java
public class GenericCsvRepositoryTest {

    @Test
    public void testSaveAndReload_persistsAllRecords() throws Exception {
        Path testDir = Files.createTempDirectory("repo_test");
        MerchandiseRepository repo = new MerchandiseRepository(testDir);
        Merchandise m = new Merchandise("T001", "Test Jersey", ProductCategory.KIT, 85.0, 100);
        repo.save(m);
        // Simulate JVM restart — create new repo instance
        MerchandiseRepository freshRepo = new MerchandiseRepository(testDir);
        Merchandise loaded = freshRepo.findById("T001");
        assertNotNull(loaded);
        assertEquals("Test Jersey", loaded.getName());
        assertEquals(100, loaded.getStockQuantity());
    }

    @Test
    public void testConcurrentReads_noLockRequired() throws Exception {
        Path testDir = Files.createTempDirectory("repo_test");
        MerchandiseRepository repo = new MerchandiseRepository(testDir);
        for (int i = 0; i < 100; i++) {
            repo.cacheOnly(new Merchandise("P" + i, "Product " + i, ProductCategory.KIT, 10.0, 10));
        }
        repo.flush();
        // Concurrent reads from 10 threads
        ExecutorService exec = Executors.newFixedThreadPool(10);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int t = 0; t < 10; t++) {
            futures.add(exec.submit(() -> repo.findAll().size()));
        }
        for (Future<Integer> f : futures) {
            assertEquals(100, f.get().intValue());
        }
        exec.shutdown();
    }
}
```

#### Pattern 3: Service Business Logic

```java
public class TicketServiceTest {

    @Test(expected = InsufficientBalanceException.class)
    public void testBookTickets_insufficientBalance_throwsBeforeLock() throws Exception {
        Fan poorFan = new Fan("F001", "Test Fan", "poor@test.com", 10.0);
        fanRepo.cacheOnly(poorFan);
        Match match = new Match("M001", "Home FC", "Away FC", LocalDate.now().plusDays(7));
        matchRepo.cacheOnly(match);
        seatRepo.flush();
        List<String> seats = Arrays.asList("A-B-001");
        ticketService.bookTickets("F001", "M001", seats, "admin");
    }

    @Test
    public void testBookTickets_exactlyFourSeats_succeeds() throws Exception {
        Fan richFan = new Fan("F002", "Rich Fan", "rich@test.com", 10000.0);
        fanRepo.cacheOnly(richFan);
        List<String> seats = Arrays.asList("A-A-001", "A-A-002", "A-A-003", "A-A-004");
        List<Ticket> tickets = ticketService.bookTickets("F002", "M001", seats, "admin");
        assertEquals(4, tickets.size());
    }

    @Test(expected = ExceedsMaxTicketsException.class)
    public void testBookTickets_fiveSeats_throws() throws Exception {
        List<String> seats = Arrays.asList("A-A-001","A-A-002","A-A-003","A-A-004","A-A-005");
        ticketService.bookTickets("F001", "M001", seats, "admin");
    }
}
```

#### Pattern 4: Security Tests

```java
public class AuthenticationManagerTest {

    @Test
    public void testVerifyPassword_correctPassword_returnsTrue() {
        String hash = authManager.hashPassword("admin123");
        assertTrue(authManager.verifyPassword("admin123", hash));
    }

    @Test
    public void testVerifyPassword_wrongPassword_returnsFalse() {
        String hash = authManager.hashPassword("admin123");
        assertFalse(authManager.verifyPassword("wrongpassword", hash));
    }

    @Test
    public void testVerifyPassword_malformedHash_returnsFalse() {
        assertFalse(authManager.verifyPassword("pw", "invalid-no-colon"));
        assertFalse(authManager.verifyPassword("pw", "a:b:c"));
    }

    @Test
    public void testVerifyPassword_nullInputs_returnsFalse() {
        assertFalse(authManager.verifyPassword(null, "salt:hash"));
        assertFalse(authManager.verifyPassword("pw", null));
    }
}

public class SecurityContextTest {

    @Test
    public void testThreadLocal_isolation_betweenThreads() throws Exception {
        SecurityContext ctx = SecurityContext.getInstance();
        AtomicReference<String> threadResult = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);

        // Thread A logs in as admin
        new Thread(() -> {
            UserAccount adminAccount = new UserAccount("admin", "", Role.ADMIN, AccountStatus.ACTIVE, "admin_id");
            UserSession adminSession = new UserSession("s1", adminAccount, null, "127.0.0.1");
            ctx.setCurrentSession(adminSession);
            threadResult.set(ctx.getCurrentSession().getUsername());
            latch.countDown();
        }).start();

        // Thread B logs in as manager
        new Thread(() -> {
            UserAccount mgrAccount = new UserAccount("manager", "", Role.QUAN_LY_QUAY, AccountStatus.ACTIVE, "mgr_id");
            UserSession mgrSession = new UserSession("s2", mgrAccount, null, "127.0.0.1");
            ctx.setCurrentSession(mgrSession);
            threadResult.set(ctx.getCurrentSession().getUsername());
            latch.countDown();
        }).start();

        latch.await(5, TimeUnit.SECONDS);
        // Each thread sees only its own session — no bleeding
        assertNotNull(ctx.getCurrentSession());
    }
}
```

---

## 3. Concurrency Core Tests

### Lock Mode Verification Matrix

| Test Case | Lock Mode | Threads | Target Seats | Expected Double-Bookings | Acceptable Threshold |
|-----------|-----------|---------|-------------|--------------------------|---------------------|
| `testNoLock_allowsDoubleBooking` | `NO_LOCK` | 50 | 10 | > 0 | Any |
| `testSynchronized_zeroDoubleBookings` | `SYNCHRONIZED` | 50 | 10 | 0 | 0 |
| `testFileLock_zeroDoubleBookings` | `FILE_LOCK` | 50 | 10 | 0 | 0 |
| `testOptimisticLock_zeroDoubleBookings` | `OPTIMISTIC` | 50 | 10 | 0 | 0 |
| `testOptimisticLock_exhaustedRetries_propagates` | `OPTIMISTIC` | 100 | 5 | 0 | < targetSeats |

### Concurrent Booking Test Harness

```java
public class ConcurrencyCoreTest {

    private TicketSimulator simulator;
    private Path dataDir;

    @Before
    public void setUp() throws Exception {
        dataDir = Files.createTempDirectory("concurrency_test");
        DataGenerator dg = new DataGenerator(dataDir);
        dg.generateAll();
        // Initialize all repositories...
        simulator = new TicketSimulator(ticketService, seatRepo, ticketRepo,
                                        matchRepo, fanRepo, transactionRepo);
    }

    @After
    public void tearDown() {
        simulator.shutdown();
    }

    @Test
    public void testNoLockMode_producesDoubleBookings() throws Exception {
        simulator.setLockMode(TicketService.LockMode.NO_LOCK);
        List<String> targetSeats = seatRepo.findAvailableSeats(10).stream()
                .map(Seat::getSeatId).collect(Collectors.toList());
        simulator.setTargetSeats(targetSeats);
        BenchmarkResult result = simulator.run();
        assertTrue("NO_LOCK should produce double-bookings under contention",
                result.getDoubleBookedCount() > 0);
        assertTrue("Success rate should be < 100% under contention",
                result.getSuccessRate() < 100.0);
    }

    @Test
    public void testSynchronizedMode_zeroDoubleBookings() throws Exception {
        simulator.setLockMode(TicketService.LockMode.SYNCHRONIZED);
        List<String> targetSeats = seatRepo.findAvailableSeats(10).stream()
                .map(Seat::getSeatId).collect(Collectors.toList());
        simulator.setTargetSeats(targetSeats);
        BenchmarkResult result = simulator.run();
        assertEquals("SYNCHRONIZED should produce zero double-bookings",
                0, result.getDoubleBookedCount());
    }

    @Test
    public void testOptimisticLock_highContention() throws Exception {
        simulator.setLockMode(TicketService.LockMode.OPTIMISTIC);
        simulator.setThreadCount(100);  // 100 threads
        List<String> targetSeats = seatRepo.findAvailableSeats(5).stream()
                .map(Seat::getSeatId).collect(Collectors.toList());
        simulator.setTargetSeats(targetSeats);
        BenchmarkResult result = simulator.run();
        assertEquals("OPTIMISTIC should produce zero double-bookings even under high contention",
                0, result.getDoubleBookedCount());
    }

    @Test
    public void testComparativeBenchmark_comparesAllModes() throws Exception {
        Map<String, BenchmarkResult> results = simulator.runComparativeBenchmark(
                seatRepo.findAvailableSeats(50).stream().map(Seat::getSeatId).collect(Collectors.toList()),
                20  // threads
        );
        assertEquals(4, results.size()); // NO_LOCK, SYNCHRONIZED, FILE_LOCK, OPTIMISTIC
        for (Map.Entry<String, BenchmarkResult> entry : results.entrySet()) {
            String mode = entry.getKey();
            BenchmarkResult r = entry.getValue();
            if (!mode.equals("NO_LOCK")) {
                assertEquals("Safe mode " + mode + " should have 0 double-bookings",
                        0, r.getDoubleBookedCount());
            }
        }
    }
}
```

### Benchmark Metrics Captured

| Metric | Definition | Formula |
|--------|------------|---------|
| **Success Rate** | Percentage of booking attempts that succeeded | `100 * successful / totalAttempts` |
| **Double-Booking Rate** | Seats with > 1 ticket issued | `count(seats with duplicate ticketIds)` |
| **Throughput** | Transactions per second | `totalAttempts / wallClockSeconds` |
| **Failure Rate** | Percentage of attempts that threw exceptions | `100 * failed / totalAttempts` |
| **Lock Contention** | Average wait time per thread | `totalWaitTimeMs / threadCount` |

---

## 4. Write-Ahead Logging (WAL) Storage Tests

### WAL Architecture Reference

```
writeWithWal(filePath, header, rows)
    │
    ├─ 1. Acquire FileLock on  {file}.lock          (exclusive OS-level)
    ├─ 2. Write all rows to    {file}.tmp            (64 KB BufferedWriter)
    ├─ 3. Copy {file} to        {file}.bak            (safety backup)
    ├─ 4. ATOMIC_MOVE          {file}.tmp  ->  {file}  (filesystem rename)
    ├─ 5. Delete                {file}.bak            (cleanup)
    └─ 6. Release FileLock / close channel

needsRecovery(filePath)  ──►  true if {file}.tmp  exists
    │
    └─ recover(filePath)  ──►  copy {file}.bak  ->  {file}
```

### WAL Test Cases

| ID | Test Case | Setup | Action | Assertion |
|----|-----------|-------|--------|-----------|
| WAL-01 | Atomic write — all rows persisted | New repo, 1,000 records | Call `saveAll()` | File line count = 1 header + 1,000 data rows |
| WAL-02 | Atomic write — partial JVM crash | Write in progress | Kill thread mid-write | `needsRecovery()` returns `true` |
| WAL-03 | Recovery — restore from `.bak` | `.tmp` exists, crash state | Call `loadAll()` | Data restored from `.bak`, `.tmp` deleted |
| WAL-04 | FileLock — concurrent write blocked | Thread A holds lock | Thread B attempts write | Thread B blocks until Thread A releases |
| WAL-05 | FileLock — same JVM multi-thread | 10 threads writing | All call `save()` concurrently | Only one proceeds; others queue and succeed |
| WAL-06 | No data loss after flush | Insert 500 records | Call `flush()` | After restart, `findAll().size()` = 500 |
| WAL-07 | Header preserved after WAL | Custom header | `saveAll()` | First line of file = exact header string |
| WAL-08 | Concurrent reads during WAL write | Thread A writing | Thread B calling `findAll()` | Thread B sees old data (cache) until write completes |

### WAL Recovery Test

```java
public class WALStorageTest {

    @Test
    public void testNeedsRecovery_returnsTrueWhenTmpExists() throws Exception {
        Path tmpDir = Files.createTempDirectory("wal_test");
        Path csvFile = tmpDir.resolve("test.csv");
        Path tmpFile = tmpDir.resolve("test.csv.tmp");

        // Create a valid CSV first
        Files.write(csvFile, "id,name\n1,Alice\n".getBytes(StandardCharsets.UTF_8));

        // Simulate interrupted write — leave .tmp behind
        Files.write(tmpFile, "id,name\n1,Bob\n".getBytes(StandardCharsets.UTF_8));

        assertTrue(WriteAheadLog.needsRecovery(csvFile));
    }

    @Test
    public void testRecover_restoresFromBackup() throws Exception {
        Path tmpDir = Files.createTempDirectory("wal_test");
        Path csvFile = tmpDir.resolve("test.csv");
        Path bakFile = tmpDir.resolve("test.csv.bak");

        // Simulate: valid data backed up, .tmp left from crash
        Files.write(csvFile, "id,name\n1,Corrupt\n".getBytes(StandardCharsets.UTF_8));
        Files.write(bakFile, "id,name\n1,Original\n".getBytes(StandardCharsets.UTF_8));
        Files.write(tmpDir.resolve("test.csv.tmp"), "id,name\n1,Corrupt\n".getBytes(StandardCharsets.UTF_8));

        WriteAheadLog.recover(csvFile);

        String recovered = new String(Files.readAllBytes(csvFile), StandardCharsets.UTF_8);
        assertTrue("Should restore from .bak", recovered.contains("Original"));
        assertFalse("Should delete .tmp after recovery", Files.exists(tmpDir.resolve("test.csv.tmp")));
    }

    @Test
    public void testAtomicMove_renamesTmpToMain() throws Exception {
        Path tmpDir = Files.createTempDirectory("wal_test");
        Path csvFile = tmpDir.resolve("atom.csv");
        Path tmpFile = tmpDir.resolve("atom.csv.tmp");

        Files.write(tmpFile, "id\n1\n".getBytes(StandardCharsets.UTF_8));
        WriteAheadLog.writeWithWal(csvFile, "id\n", Arrays.asList("1"));

        assertTrue("Main file exists", Files.exists(csvFile));
        assertFalse("Tmp file deleted", Files.exists(tmpFile));
        assertEquals("id\n1\n", new String(Files.readAllBytes(csvFile), StandardCharsets.UTF_8));
    }
}
```

---

## 5. Data Reset & Environment Setup

### Quick Reference: Reset Scenarios

| Scenario | Scope | Command |
|----------|--------|---------|
| **Full reset** | All 16 CSV files | `Remove-Item data\*.csv,*lock,*tmp,*bak -Force` |
| **Merchandise catalog** | `merchandise.csv` only | `Remove-Item data\merchandise.csv,... -Force` |
| **Seat state reset** | `stadium_seats.csv`, `tickets.csv` | `Remove-Item data\stadium_seats.csv,...; Remove-Item data\tickets.csv,...` |
| **Transactions** | `transactions.csv` only | `Remove-Item data\transactions.csv,... -Force` |

### Full Data Reset

```powershell
cd c:\Users\Acer\Documents\TicketBooking

# Remove all CSV data and WAL sidecar files
Remove-Item -Path "data\*.csv" -Force
Remove-Item -Path "data\*.lock" -Force
Remove-Item -Path "data\*.tmp" -Force
Remove-Item -Path "data\*.bak" -Force

# Verify clean state
Get-ChildItem "data" | Select-Object Name, Length

# Recompile and run
javac -d out -sourcepath src -cp "src" src/com/club/ClubApplication.java
java -cp out com.club.ClubApplication
```

### Data Generation Products Reference

| Category | Product | Price | Initial Stock |
|----------|---------|-------|--------------|
| KIT | PROD_001 Official Home Jersey 2026 | 85.00 | 150 |
| KIT | PROD_002 Official Away Jersey 2026 | 85.00 | 120 |
| KIT | PROD_003 Pro Training Tracksuit | 110.00 | 75 |
| FOOTWEAR | PROD_004 Elite Football Boots (Spikes) | 145.00 | 45 |
| FOOTWEAR | PROD_005 Classic Lifestyle Sneakers | 95.00 | 60 |
| ACCESSORIES | PROD_006 Matchday Grip Socks | 15.00 | 400 |
| ACCESSORIES | PROD_007 Club Premium Snapback Cap | 30.00 | 200 |

---

## 6. Running the Application

### Prerequisites

- Java 8 JDK (`java -version` confirms `1.8.x`)
- Zero external dependencies — pure standard library
- Working directory: `c:\Users\Acer\Documents\TicketBooking\`

### Compile & Run

```bash
# Compile
javac -d out -sourcepath src -cp "src" src/com/club/ClubApplication.java

# Run
java -cp out com.club.ClubApplication
```

### Default Credentials

| Username | Password | Role | Key Permissions |
|----------|----------|------|----------------|
| `admin` | `admin123` | `ADMIN` | ALL |
| `manager` | `manager123` | `QUAN_LY_QUAY` | Process tickets, view map |
| `coach` | `coach123` | `HLV_TRUONG` | Update stats, manage fitness |
| `hr_director` | `hr123` | `GIAM_DOC_NHAN_SU` | HR, modify salaries, reports |
| `finance_director` | `finance123` | `GIAM_DOC_TAI_CHINH` | Financial reports, payroll |
| `commentator` | `commentator123` | `TRONG_TAI` | Update stats, manage league |

---

*Document Version: 1.0 — FCM-ERP Testing & QA Guide — May 2026*
