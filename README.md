# 🎫 Stadium Ticket Booking Simulation

## 📌 Project Information

**Course:** LAB211 — OOP with Java
**Project Type:** Group Project
**Architecture:** MVC (Model–View–Controller)
**Data Storage:** CSV Files
**Programming Language:** Java

---

## 👥 Team Members

| No. | Student ID | Full Name          |        
| --- | ---------- | -------------------|
| 1   | `QE200050` |  Huỳnh Trường Phát |    
| 2   | `QE200004` |  Võ Ngọc Phú       |  
| 3   | `QE200077` |  Châu Minh Tiến    |    
---

## 📖 Project Overview

**Stadium Ticket Booking Simulation** is a Java-based application that simulates an online football stadium ticket booking system.

The system allows fans to:

* Register and log in to the system.
* Browse stadiums, sections, matches, and available seats.
* Select and book tickets.
* View their booked tickets.
* Process booking transactions.
* Simulate multiple users booking tickets concurrently.

All data is stored in CSV files instead of using a relational database.

The main challenge of this project is to prevent **Double Booking** when multiple threads attempt to book the same seat at the same time.

---

## 🔬 Research Question

> **Which synchronization mechanism can prevent Double Booking when thousands of Fan Threads attempt to book tickets concurrently, while maintaining acceptable system performance?**

The project implements and compares different synchronization mechanisms, including:

* `NO_LOCK`
* `SYNCHRONIZED`
* `FILE_LOCK`
* `OPTIMISTIC LOCKING`

The performance of each mechanism is evaluated based on:

* Throughput (transactions per second)
* Number of successful bookings
* Number of failed bookings
* Double Booking Rate
* Execution time
* Optimistic Locking retries

---

## 🎯 Project Objectives

The main objectives of this project are:

1. Build a complete Java OOP ticket booking simulation system.
2. Apply the MVC architectural pattern.
3. Store and manage large amounts of data using CSV files.
4. Implement CRUD operations for the main entities.
5. Generate at least 10,000 valid data records.
6. Simulate concurrent ticket booking using multiple threads.
7. Implement and compare different synchronization mechanisms.
8. Prevent Double Booking and maintain data integrity.
9. Measure and analyze system performance under high concurrency.

---

## 🏗️ System Architecture

The project follows the **MVC architecture**.

```text
┌──────────────────────┐
│        VIEW          │
│  Console User Interface│
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│     CONTROLLER       │
│  Application Flow    │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│        MODEL         │
│ Entities + Business  │
│ Logic + Validation   │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│     REPOSITORY       │
│      CSV Storage     │
└──────────────────────┘
```

### Model

Responsible for:

* Entity classes
* Business rules
* Data validation
* Seat status management

### Controller

Responsible for:

* Handling user requests
* Coordinating application flow
* Calling services and repositories

### View

Responsible for:

* Displaying menus
* Receiving user input
* Displaying results and reports

### Repository

Responsible for:

* Reading CSV files
* Writing CSV files
* CRUD operations
* Searching and filtering data

---

## 📂 Project Structure

```text
src/
└── com/
    └── club/
        ├── model/
        ├── repository/
        ├── service/
        ├── controller/
        ├── security/
        ├── simulator/
        ├── ui/
        ├── util/
        └── exception/

data/
├── stadiums.csv
├── sections.csv
├── seats.csv
├── fans.csv
├── players.csv
├── staff.csv
├── matches.csv
├── tickets.csv
├── transactions.csv
└── ...

docs/
├── report.docx
├── slide.pptx
├── class_diagram.png
└── flowcharts/

ai_logs/
├── member1_ai_log.md
├── member2_ai_log.md
└── member3_ai_log.md

README.md
```

---

## 🧩 Main System Entities

The main entities of the system include:

* **Stadium** — Stores stadium information.
* **Section** — Represents different areas of the stadium.
* **Seat** — Represents seats and their availability status.
* **Match** — Stores football match information.
* **Fan** — Represents users who purchase tickets.
* **Ticket** — Stores ticket information.
* **Transaction** — Stores booking transaction information.

### Seat Status

A seat can have the following states:

```text
AVAILABLE → LOCKED → BOOKED
```

Once a seat becomes `BOOKED`, it cannot be sold to another fan.

---

## 🔒 Synchronization Mechanisms

The system implements multiple synchronization strategies.

### 1. NO_LOCK

No synchronization mechanism is applied.

```text
Read Seat
    ↓
Check Availability
    ↓
Book Seat
```

This mechanism is used as a baseline and may result in **Double Booking** under high concurrency.

---

### 2. SYNCHRONIZED

Java's `synchronized` mechanism is used to ensure that only one thread can access the critical booking section at a time.

```text
Thread 1 → Lock → Check → Book → Unlock
Thread 2 → Wait
Thread 3 → Wait
```

This mechanism prioritizes data consistency.

---

### 3. FILE_LOCK

Java NIO `FileLock` is used to coordinate access to the CSV file.

This mechanism allows different processes or threads to synchronize access to shared file data.

---

### 4. OPTIMISTIC LOCKING

The system uses a `version` field to detect conflicts.

```text
1. Read Seat + Version
2. Check Availability
3. Update only if Version is unchanged
4. If conflict occurs → Retry
```

Example:

```text
Thread A reads Version = 5
Thread B reads Version = 5

Thread A updates → Version = 6
Thread B detects conflict → Retry
```

---

## 🚀 Simulator

The Simulator is used to test the system under high concurrency.

It uses:

* `ExecutorService`
* `CountDownLatch`
* Multiple concurrent threads
* Configurable number of attacks
* Multiple synchronization modes

Example:

```text
Thread Count:      1000
Seats / Thread:    1
Attacks / Thread:  1
Lock Mode:         SYNCHRONIZED
```

The Simulator measures:

| Metric               | Description                       |
| -------------------- | --------------------------------- |
| Success Count        | Number of successful bookings     |
| Failure Count        | Number of failed booking attempts |
| Double Booking Count | Number of duplicated bookings     |
| Throughput           | Transactions per second           |
| Execution Time       | Total simulation time             |
| Optimistic Retries   | Number of retry operations        |

---

## 🛠️ Technologies Used

* Java
* Object-Oriented Programming
* MVC Architecture
* Java Collections Framework
* Java Concurrency
* `ExecutorService`
* `CountDownLatch`
* `synchronized`
* Java NIO `FileLock`
* Optimistic Locking
* CSV File I/O
* JUnit Testing
* Git / GitHub

---

## ▶️ How to Run the Project

### 1. Clone the Repository

```bash
git clone <YOUR_REPOSITORY_URL>
```

Navigate to the project directory:

```bash
cd <PROJECT_FOLDER>
```

---

### 2. Compile the Project

If using a Java IDE such as NetBeans or IntelliJ IDEA:

1. Open the project.
2. Make sure the JDK is correctly configured.
3. Build the project.
4. Run the main class.

For command line compilation:

```bash
javac -d bin src/**/*.java
```

> The exact compilation command may depend on the project structure and operating system.

---

### 3. Generate Sample Data

Run the data generator:

```text
DataGenerator
```

The generator creates valid CSV data for the system.

The generated data includes:

* Stadiums
* Sections
* Seats
* Fans
* Matches
* Tickets
* Transactions

The project generates at least **10,000 records** in total.

---

### 4. Run the Main Application

Run:

```text
Main
```

The application provides a console-based interface for:

* Fan registration
* Login
* Viewing matches
* Viewing seats
* Booking tickets
* Viewing tickets
* Viewing booking information

---

### 5. Run the Concurrency Simulator

Run:

```text
TicketSimulator
```

The simulator tests different synchronization modes:

```text
NO_LOCK
SYNCHRONIZED
FILE_LOCK
OPTIMISTIC
```

Example experiment:

```text
Thread Pool: 1000 threads
Seats per Thread: 1
Attacks per Thread: 1
```

The result is displayed in a comparison table.

---

## 🧪 Testing

The project includes unit tests for important system components.

Testing focuses on:

* Entity validation
* CSV serialization and deserialization
* Repository CRUD operations
* Data generation
* Ticket booking
* Seat availability
* Concurrent booking
* Synchronization mechanisms

Example:

```text
✓ Entity tests passed
✓ Repository tests passed
✓ CSV loading tests passed
✓ Booking tests passed
✓ Concurrency tests passed
```

---

## 📊 Expected Research Result

The project compares the performance and correctness of different synchronization mechanisms.

The expected results include:

| Mechanism    | Double Booking | Throughput  | Main Advantage             |
| ------------ | -------------- | ----------- | -------------------------- |
| NO_LOCK      | Possible       | High        | Fast but unsafe            |
| SYNCHRONIZED | 0%             | Medium      | Simple and reliable        |
| FILE_LOCK    | 0%             | Lower       | File-level synchronization |
| OPTIMISTIC   | 0%             | High/Medium | Good concurrency           |

The final conclusion is based on experimental results collected from the Simulator.

---

## 📚 Documentation

The project documentation includes:

* UML Class Diagram
* Booking Flowchart
* Synchronization Flowchart
* Simulator Flowchart
* Project Report
* Presentation Slides
* AI Logs
* AI Reflection

---

## 🤖 AI Usage

Each team member maintains an individual AI Log documenting:

* Prompts submitted to AI
* AI-generated suggestions
* Problems found in AI output
* Code modifications made by the team
* Verification and testing results

AI is used as a supporting tool for:

* Understanding technical concepts
* Debugging
* Generating ideas
* Improving code structure
* Analyzing concurrency problems

All final code and decisions are reviewed and verified by the project team.

---

**Concurrency is hard. Understanding why it is hard is the real lesson.**
