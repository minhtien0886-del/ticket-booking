
## Table of Contents

1. [Test Execution Guide](#1-test-execution-guide)
2. [Test Cases](#2-test-cases)
3. [Summary & Sign-Off](#3-summary--sign-off)

---

## 1. Test Execution Guide

### Pre-Conditions

Before executing any test case:

1. **Reset data:** Delete all CSV files in `data/` folder
2. **Compile:** Run `javac -d out -sourcepath src -cp "src" src/com/club/ClubApplication.java`
3. **Launch:** Run `java -cp out com.club.ClubApplication`
4. **Verify:** Application banner and login prompt appear

### Session State

Each test case begins from a clean startup. Log out and restart the application between test cases to ensure session isolation.

### Test Case Format

| Field | Description |
|-------|-------------|
| **Test ID** | Unique identifier (e.g., TC-LOGIN-01) |
| **Priority** | P0 = Critical, P1 = High, P2 = Medium |
| **Category** | Authentication / RBAC / Booking / Shop / Simulator / Reporting |
| **Pre-conditions** | Setup required before executing the test |
| **Test Steps** | Numbered, sequential actions |
| **Expected Result** | Observable outcome for each step |
| **Pass / Fail** | Tester marks outcome |

---

## 2. Test Cases

---

### TC-LOGIN-01: Multi-Role Login — All Six Roles

| Field | Value |
|-------|-------|
| **Test ID** | `TC-LOGIN-01` |
| **Priority** | P0 — Critical |
| **Category** | Authentication |

**Objective:** Verify that each of the six system roles can authenticate successfully and receives the correct role-adaptive menu.

**Pre-conditions:** Application started from clean state. No prior login.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Enter username: `admin`, password: `admin123` | Login succeeds. Admin menu displayed with all options. |
| 2 | Logout | Session terminated. Login prompt returned. |
| 3 | Enter username: `manager`, password: `manager123` | Login succeeds. Manager menu displayed (ticket processing, stadium map). |
| 4 | Logout | Session terminated. |
| 5 | Enter username: `coach`, password: `coach123` | Login succeeds. Coach menu displayed (player fitness, stats). |
| 6 | Logout | Session terminated. |
| 7 | Enter username: `hr_director`, password: `hr123` | Login succeeds. HR menu displayed (personnel reports, salary). |
| 8 | Logout | Session terminated. |
| 9 | Enter username: `finance_director`, password: `finance123` | Login succeeds. Finance menu displayed (financial reports, payroll). |
| 10 | Logout | Session terminated. |
| 11 | Enter username: `commentator`, password: `commentator123` | Login succeeds. Commentator menu displayed (stats, league). |

**Pass Criteria:** All 6 logins succeed with role-appropriate menus. No authentication errors. Session context correctly established for each role.

**Fail Condition:** Any role fails to authenticate with correct credentials. Menu items visible that should be hidden for that role.

---

### TC-LOGIN-02: Login Failure — Invalid Credentials

| Field | Value |
|-------|-------|
| **Test ID** | `TC-LOGIN-02` |
| **Priority** | P0 — Critical |
| **Category** | Authentication / Negative |

**Objective:** Verify that the system rejects invalid usernames and passwords with appropriate error messages.

**Pre-conditions:** Application started. No active session.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Enter username: `nonexistent`, password: `admin123` | Error: "Account not found." Login prompt re-displayed. |
| 2 | Enter username: `admin`, password: `wrongpassword` | Error: "Invalid password." Login prompt re-displayed. |
| 3 | Enter username: `admin`, password: (empty) | Error: "Password cannot be empty." Login prompt re-displayed. |
| 4 | Enter username: (empty), password: `admin123` | Error: "Username cannot be empty." Login prompt re-displayed. |
| 5 | Enter username: `ADMIN`, password: `admin123` | Login succeeds or error "Account not found." (case sensitivity on username) |

**Pass Criteria:** All 4 invalid attempts rejected with descriptive error messages. No session created. No stack trace displayed to end user.

**Fail Condition:** Invalid credentials accepted. Stack trace visible. Session created on bad credentials.

---

### TC-RBAC-01: FAN Role — Denied Access to Management Interfaces

| Field | Value |
|-------|-------|
| **Test ID** | `TC-RBAC-01` |
| **Priority** | P0 — Critical |
| **Category** | RBAC |

**Objective:** Verify that a FAN account cannot access any staff or administrative functions. FAN role is strictly confined to purchasing and viewing.

**Pre-conditions:** FAN account created or existing. Logged in as FAN.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Attempt to navigate to Staff Management / HR | `AccessDeniedException` — FAN has no `MANAGE_HUMAN_RESOURCE` permission. |
| 2 | Attempt to navigate to Salary Modification | `AccessDeniedException` — FAN has no `MODIFY_SALARY` permission. |
| 3 | Attempt to view Financial Reports | `AccessDeniedException` — FAN has no `VIEW_FINANCIAL_REPORTS` permission. |
| 4 | Attempt to access Ticket Simulator | `AccessDeniedException` — FAN has no `RUN_SIMULATOR` permission. |
| 5 | Attempt to purchase a ticket | ALLOWED — FAN has `PURCHASE_TICKET` permission. |
| 6 | Attempt to view stadium seat map | ALLOWED — FAN has `VIEW_SEAT_MAP` permission. |
| 7 | Attempt to view matches | ALLOWED — FAN has `VIEW_MATCHES` permission. |

**Pass Criteria:** All 4 restricted operations blocked with `AccessDeniedException`. All 3 allowed operations succeed. Error messages are user-friendly (no Java stack traces in CLI output).

**Fail Condition:** FAN can access any staff-only function. Role boundary is not enforced.

---

### TC-RBAC-02: Coach (HLV_TRUONG) — Fitness vs. Salary Boundary

| Field | Value |
|-------|-------|
| **Test ID** | `TC-RBAC-02` |
| **Priority** | P1 — High |
| **Category** | RBAC |

**Objective:** Verify that a coach can update player fitness but is strictly blocked from modifying salaries.

**Pre-conditions:** Logged in as `coach` / `coach123`.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Navigate to Player Management | ALLOWED — coach has fitness management access. |
| 2 | Update player fitness to 95 for `PLAYER_0001` | Update succeeds. Confirmation displayed. |
| 3 | Attempt to modify `PLAYER_0001` salary | `AccessDeniedException` — coach lacks `MODIFY_SALARY` permission. |
| 4 | Attempt to modify `STAFF_0001` salary | `AccessDeniedException` — same restriction applies. |
| 5 | Attempt to hire a new player | `AccessDeniedException` — `MANAGE_HUMAN_RESOURCE` not assigned to coach. |
| 6 | Attempt to view financial summary report | `AccessDeniedException` — `VIEW_FINANCIAL_REPORTS` not assigned to coach. |
| 7 | View player list | ALLOWED — coach has `VIEW_MATCHES`. |

**Pass Criteria:** Fitness update succeeds. All 5 restricted operations blocked. Coach role boundary is enforced correctly.

**Fail Condition:** Coach can modify salaries or access HR reports. Permission boundary breached.

---

### TC-RBAC-03: Finance Director (GIAM_DOC_TAI_CHINH) — View vs. Modify Boundary

| Field | Value |
|-------|-------|
| **Test ID** | `TC-RBAC-03` |
| **Priority** | P1 — High |
| **Category** | RBAC |

**Objective:** Verify that the Finance Director can view financial reports and process payroll but cannot directly modify salary records.

**Pre-conditions:** Logged in as `finance_director` / `finance123`.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | View Financial Summary Report | ALLOWED — finance director has `VIEW_FINANCIAL_REPORTS`. |
| 2 | View Personnel / Payroll Report | ALLOWED — finance director has `VIEW_FINANCIAL_REPORTS`. |
| 3 | Attempt to modify player salary | `AccessDeniedException` — finance director lacks `MODIFY_SALARY`. |
| 4 | Attempt to modify staff salary | `AccessDeniedException` — same restriction. |
| 5 | Attempt to process a ticket | `AccessDeniedException` — finance director lacks `PROCESS_TICKET` and `PURCHASE_TICKET`. |
| 6 | Process payroll run for current period | ALLOWED — finance director has `PROCESS_PAYROLL`. |

**Pass Criteria:** All financial viewing and payroll processing succeed. All salary modifications and ticket operations are blocked. Clean separation of read vs. write access.

**Fail Condition:** Finance director can modify salary records. Permission assignment in `Role.java` is incorrect.

---

### TC-BOOK-01: Booking Limit — Max 4 Tickets Per Transaction

| Field | Value |
|-------|-------|
| **Test ID** | `TC-BOOK-01` |
| **Priority** | P0 — Critical |
| **Category** | Booking / Business Rules |

**Objective:** Verify that the system enforces the maximum of 4 tickets per booking transaction.

**Pre-conditions:** Logged in as FAN with balance >= 1,000. Match with `TICKETS_ON_SALE` status available. Stadium map accessible.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Navigate to ticket booking | Ticket booking flow accessible. |
| 2 | Select a match | Match details displayed. |
| 3 | Select 4 available seats: A-B-S001, A-B-S002, A-B-S003, A-B-S004 | 4 seats confirmed. |
| 4 | Confirm booking | 4 tickets issued. Confirmation displayed. Balance deducted. |
| 5 | Attempt to select 5 seats: A-B-S001 through A-B-S005 | Error: `ExceedsMaxTicketsException`. Booking rejected before any seat is locked. |
| 6 | Verify no seats were locked in step 5 | All seats remain in original status. Balance unchanged. |

**Pass Criteria:** 4-seat booking succeeds. 5-seat booking is rejected atomically — no partial locks, no balance deduction.

**Fail Condition:** 5+ seats booked in a single transaction. Partial lock of seats before rejection. Balance deducted on failed booking.

---

### TC-BOOK-02: Insufficient Balance — Rejection Before Lock

| Field | Value |
|-------|-------|
| **Test ID** | `TC-BOOK-02` |
| **Priority** | P0 — Critical |
| **Category** | Booking / Business Rules |

**Objective:** Verify that a fan with insufficient balance cannot book seats and that no seat is locked during the failed attempt.

**Pre-conditions:** Logged in as FAN with balance = 50.00. VIP seats priced at 500.00 each.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Navigate to ticket booking | Booking flow accessible. |
| 2 | Select a VIP match (seats at 500.00 each) | VIP match confirmed. |
| 3 | Attempt to book 1 VIP seat (total: 500.00) | `InsufficientBalanceException`. Booking rejected. |
| 4 | Verify seat is still `AVAILABLE` | Seat status unchanged. |
| 5 | Verify fan balance is unchanged | Balance remains 50.00. |
| 6 | Deposit 500.00 to fan account | Deposit succeeds. Balance = 550.00. |
| 7 | Book the same VIP seat | Booking succeeds. Ticket issued. Balance = 50.00. |

**Pass Criteria:** Insufficient balance rejected before any seat state change. No partial transactions. Seat remains available for other fans.

**Fail Condition:** Seat locked or booked despite insufficient balance. Balance deducted prematurely.

---

### TC-BOOK-03: Atomic Rollback — Failure Mid-Booking

| Field | Value |
|-------|-------|
| **Test ID** | `TC-BOOK-03` |
| **Priority** | P1 — High |
| **Category** | Booking / Atomicity |

**Objective:** Verify that if a multi-seat booking fails partway through (e.g., seat 2 of 3 is not available), all previously locked seats are rolled back to `AVAILABLE`.

**Pre-conditions:** Logged in as FAN with balance >= 1,000. Match selected. Seat A-B-S001 and A-B-S002 are `AVAILABLE`. Seat A-B-S003 is `BOOKED`.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Attempt to book 3 seats: A-B-S001, A-B-S002, A-B-S003 | Booking fails on third seat. |
| 2 | Verify A-B-S001 is `AVAILABLE` | Seat rolled back to `AVAILABLE`. |
| 3 | Verify A-B-S002 is `AVAILABLE` | Seat rolled back to `AVAILABLE`. |
| 4 | Verify A-B-S003 is unchanged | Remains `BOOKED` (was never touched). |
| 5 | Verify no ticket created | `tickets.csv` unchanged. |
| 6 | Verify fan balance unchanged | No deduction occurred. |

**Pass Criteria:** All seats locked before the failure are released. Atomic rollback is all-or-nothing. No orphaned `LOCKED` seats remain.

**Fail Condition:** Seat A-B-S001 or A-B-S002 remains `LOCKED` after failure. Partial ticket written. Balance partially deducted.

---

### TC-SHOP-01: Merchandise Catalog — Seed, Browse, and Checkout

| Field | Value |
|-------|-------|
| **Test ID** | `TC-SHOP-01` |
| **Priority** | P1 — High |
| **Category** | Shop / Inventory |

**Objective:** Verify the full merchandise shopping flow: catalog seed, browsing, cart management, checkout, and stock deduction.

**Pre-conditions:** `merchandise.csv` absent or empty. Logged in as FAN with balance >= 300.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Start application — trigger data seed | Console shows: "Seeding merchandise catalog with official product lineup." |
| 2 | Navigate to Merchandise Shop | 7 products displayed across KIT, FOOTWEAR, ACCESSORIES categories. |
| 3 | View KIT category | PROD_001, PROD_002, PROD_003 visible with prices 85.00, 85.00, 110.00. |
| 4 | Add PROD_001 (Home Jersey) qty=2 to cart | Cart updated: Jersey x 2 = 170.00. |
| 5 | Add PROD_006 (Grip Socks) qty=3 to cart | Cart updated: Socks x 3 = 45.00. Cart total = 215.00. |
| 6 | Check PROD_006 stock before checkout | Initial stock = 400 (from seed). |
| 7 | Proceed to checkout | Checkout confirms order. Transaction recorded. |
| 8 | Verify PROD_006 stock after checkout | Stock = 397 (400 - 3). |
| 9 | Verify `transactions.csv` has new row | `type=MERCHANDISE_SALE`, `amount=215.00`, `category=MERCHANDISE`. |
| 10 | Verify fan balance deducted | Correct amount subtracted from account. |

**Pass Criteria:** Catalog seeded with 7 products. All 3 categories (KIT, FOOTWEAR, ACCESSORIES) have correct products. Stock decremented correctly after checkout. Transaction audit trail written to `transactions.csv`.

**Fail Condition:** Catalog not seeded. Stock not decremented or decremented incorrectly. Transaction not recorded.

---

### TC-SIM-01: Multi-Threaded Ticket Simulator Benchmark

| Field | Value |
|-------|-------|
| **Test ID** | `TC-SIM-01` |
| **Priority** | P0 — Critical |
| **Category** | Concurrency / Simulator |

**Objective:** Execute the `TicketSimulator` comparative benchmark across all four locking modes and verify that safe modes (SYNCHRONIZED, FILE_LOCK, OPTIMISTIC) produce zero double-bookings while NO_LOCK produces measurable double-bookings.

**Pre-conditions:** Logged in as `admin` or `manager` (requires `RUN_SIMULATOR` permission). Application fully initialized.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Navigate to Admin Menu | Admin options displayed including "Run Ticket Simulator". |
| 2 | Select "Run Comparative Benchmark" | Simulator runs all 4 lock modes in sequence on the same 50-seat set with 20 threads. |
| 3 | Wait for all 4 runs to complete | Benchmark completes. ASCII comparison table displayed. |
| 4 | Inspect `NO_LOCK` results | Double-bookings > 0. Success rate < 100%. Confirms unsafe baseline. |
| 5 | Inspect `SYNCHRONIZED` results | Double-bookings = 0. Success rate = 100%. Throughput noted. |
| 6 | Inspect `FILE_LOCK` results | Double-bookings = 0. Success rate = 100%. Throughput noted. |
| 7 | Inspect `OPTIMISTIC` results | Double-bookings = 0. Success rate = 100%. Throughput noted. |
| 8 | Run individual `OPTIMISTIC` test with 100 threads on 5 seats | Double-bookings = 0. All 5 seats correctly booked once each. |
| 9 | Check `tickets.csv` after `OPTIMISTIC` 100-thread run | Exactly 5 tickets issued. No duplicate seat IDs. |
| 10 | Check `benchmark_results.csv` file | All metrics (throughput, success rate, double-booking count) recorded per mode. |

**Expected Comparative Results (illustrative):**

| Lock Mode | Threads | Target Seats | Success | Failed | Double-Booked | Throughput |
|-----------|---------|-------------|---------|--------|--------------|-----------|
| `NO_LOCK` | 20 | 50 | < 50 | > 0 | > 0 | Highest |
| `SYNCHRONIZED` | 20 | 50 | 50 | 0 | 0 | Lowest |
| `FILE_LOCK` | 20 | 50 | 50 | 0 | 0 | Low |
| `OPTIMISTIC` | 20 | 50 | 50 | 0 | 0 | Highest (safe) |

**Pass Criteria:**
- `NO_LOCK`: Double-bookings > 0 (baseline unsafe confirmed)
- `SYNCHRONIZED`: 0 double-bookings, 0 failures, correct success count
- `FILE_LOCK`: 0 double-bookings, 0 failures, correct success count
- `OPTIMISTIC`: 0 double-bookings, 0 failures, correct success count
- `benchmark_results.csv` exists and contains all 4 mode results

**Fail Condition:** Any safe mode (SYNCHRONIZED, FILE_LOCK, OPTIMISTIC) shows > 0 double-bookings. Benchmark results not persisted. `OPTIMISTIC` exhausts retries on any seat.

---

## 3. Summary & Sign-Off

### Test Execution Summary

| Test ID | Priority | Category | Result | Tester | Date |
|---------|----------|----------|--------|--------|------|
| TC-LOGIN-01 | P0 | Authentication | PASS / FAIL | | |
| TC-LOGIN-02 | P0 | Authentication | PASS / FAIL | | |
| TC-RBAC-01 | P0 | RBAC | PASS / FAIL | | |
| TC-RBAC-02 | P1 | RBAC | PASS / FAIL | | |
| TC-RBAC-03 | P1 | RBAC | PASS / FAIL | | |
| TC-BOOK-01 | P0 | Booking | PASS / FAIL | | |
| TC-BOOK-02 | P0 | Booking | PASS / FAIL | | |
| TC-BOOK-03 | P1 | Booking | PASS / FAIL | | |
| TC-SHOP-01 | P1 | Shop | PASS / FAIL | | |
| TC-SIM-01 | P0 | Concurrency | PASS / FAIL | | |

### Priority Summary

| Priority | Total | Passed | Failed | Blocked |
|----------|-------|--------|--------|---------|
| P0 — Critical | 5 | | | |
| P1 — High | 5 | | | |
| P2 — Medium | 0 | | | |

### Defects Found

| Defect ID | Test ID | Description | Severity | Status |
|-----------|---------|-------------|----------|--------|
| (None — fill on discovery) | | | | |

### Sign-Off

| Role | Name | Signature | Date |
|------|------|----------|------|
| Test Lead | | | |
| QA Manager | | | |
| Tech Lead | | | |

---

*Document Version: 1.0 — FCM-ERP Manual Test Scenarios — May 2026*
