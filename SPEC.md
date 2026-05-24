# Football Club Management & High-Concurrency Ticketing ERP System
## Technical Specification Document — Version 1.0

---

## 1. Project Overview

### Project Name
**FCM-ERP** — Football Club Management & Ticketing System

### Core Functionality
A production-grade, thread-safe enterprise system for managing a football club's operations including player/staff HR, stadium seat management, high-concurrency ticket booking, merchandise sales, and financial reporting. Built with pure Java 8 standard libraries, no external frameworks.

### Target Scale
- 10,000+ lines of pure Java code
- 16 CSV data files as primary persistence
- Support for 1,000+ concurrent booking threads
- Sub-500ms read performance for 10,000+ records

---

## 2. Architecture

### Package Structure
```
src/com/club/
├── model/           # Domain entities, Enums, state machines
├── security/        # Authentication, Session, RBAC
├── repository/     # Generic + concrete CSV persistence
├── service/        # Business logic services
├── controller/     # Request controllers / command interceptors
├── simulator/      # Load testing engine
├── exception/      # Custom exception hierarchy
├── ui/             # CLI menu system
└── util/           # Utilities (DataGenerator, WAL, etc.)
```

### 16 CSV Data Files
1. `accounts.csv` — User accounts
2. `fans.csv` — Fan profiles
3. `players.csv` — Player profiles
4. `staff.csv` — Staff profiles
5. `matches.csv` — Match schedule
6. `stadium_seats.csv` — Seat inventory
7. `tickets.csv` — Issued tickets
8. `transactions.csv` — Financial transactions
9. `merchandise.csv` — Product catalog
10. `cart_items.csv` — Shopping cart items
11. `salaries.csv` — Salary records
12. `league_table.csv` — League standings
13. `team_lineups.csv` — Match lineups
14. `attendance_records.csv` — Match attendance
15. `loyalty_points.csv` — Loyalty program
16. `audit_log.csv` — System audit trail

---

## 3. RBAC Security Model

### Permissions
`MANAGE_LEAGUE`, `UPDATE_LIVE_STATS`, `MODIFY_SALARY`, `PROCESS_TICKET`, `VIEW_FINANCIAL_REPORTS`, `MANAGE_HUMAN_RESOURCE`, `RUN_SIMULATOR`, `VIEW_SEAT_MAP`

### Roles & Permission Mapping
| Role | Permissions |
|------|------------|
| HLV_TRUONG | UPDATE_LIVE_STATS, VIEW_SEAT_MAP |
| GIAM_DOC_NHAN_SU | MANAGE_HUMAN_RESOURCE, MODIFY_SALARY, VIEW_SEAT_MAP |
| TRONG_TAI | UPDATE_LIVE_STATS, VIEW_SEAT_MAP, MANAGE_LEAGUE |
| QUAN_LY_QUAY | PROCESS_TICKET, VIEW_SEAT_MAP |
| GIAM_DOC_TAI_CHINH | VIEW_FINANCIAL_REPORTS, MANAGE_HUMAN_RESOURCE |
| ADMIN | ALL PERMISSIONS |
| FAN | VIEW_SEAT_MAP, VIEW_MATCHES, (ticket/merchandise purchase) |

---

## 4. Concurrency Model

### Three Locking Mechanisms
1. **NO_LOCK** — Baseline, no synchronization
2. **FILE_LOCK** — Java NIO FileChannel + FileLock on stadium_seats.csv
3. **SYNCHRONIZED_BLOCK** — Memory-level synchronized blocks
4. **OPTIMISTIC_LOCKING** — Version field with 3x exponential back-off retry

### Seat State Machine
`AVAILABLE -> LOCKED -> BOOKED` (with rollback on failure)

### Transaction Limits
- Max 4 tickets per booking transaction
- WAL (Write-Ahead Logging) for crash recovery

---

## 5. UI/CLI Architecture

### Menu Levels
1. Login/Registration
2. Main Dashboard (role-based menu items)
3. Stadium Seat Map (ASCII art)
4. Ticket Booking Flow
5. Merchandise Shop
6. Admin Panels (HR, Finance, Simulator)
7. Reports & Analytics

---

## 6. Acceptance Criteria

- [ ] All 16 CSV files populated with 10,000+ records on first run
- [ ] GenericCsvRepository loads 10,000 records in <500ms
- [ ] No duplicate usernames or seat IDs allowed
- [ ] FAN role cannot access admin/staff endpoints (AccessDeniedException)
- [ ] Three locking mechanisms produce different double-booking rates
- [ ] TicketSimulator spawns 100-1,000 concurrent threads
- [ ] Benchmark results logged to benchmark_results.csv
- [ ] ASCII stadium map renders seat states with color coding
- [ ] Shopping cart supports merchandise with variants
- [ ] All code compiles with Java 8 without external dependencies

---

*Generated: May 2026 — Enterprise Java Architecture*
