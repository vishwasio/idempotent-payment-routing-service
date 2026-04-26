# 🧭 Idempotent Payment Routing Service

###  *Reliable. Fault-tolerant. Exactly-once.* ⚡

*A production-grade backend system ensuring **idempotent**, **exactly-once payment processing** with Outbox Pattern, Dead-Letter Queue, Retry Logic, Async Gateway Simulation, Dockerized Infrastructure & CI.*

![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-red?style=flat-square)
![SpringBoot](https://img.shields.io/badge/SpringBoot-3.5.7-green?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-ready-0db7ed?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-lightgrey?style=flat-square)


> ### 🔄 Repository Migration Notice
> This project has been re-uploaded into a new repository (this one) as part of a clean and intentional migration.
>
> The previous repo contained early-stage experimental code, temporary scaffolding, and an evolving
architecture that no longer reflected the final production-ready implementation. To ensure a clean
commit history, better maintainability, and a more polished presentation for contributors and
recruiters, the entire codebase has been freshly reorganized and published here.
> 
> This repository is now the main and actively maintained version of the Idempotent Payment
Routing Service. All future enhancements, issues, documentation updates, and feature development
will take place here.


# 🚀 Overview

This service handles **payment routing** with guaranteed **idempotency** and **exactly-once semantics** even under:

- duplicate client requests
- retries
- backend crashes
- partial failures
- async gateway failures

It uses **Outbox Pattern**, **Dead-Letter Queue**, and **Retry Scheduling**, making it behave like a **mini-payment-gateway pre-processor**.

Perfect for fintech, e-commerce, and microservices where payment consistency is critical.

---

# 🧩 Why This Project Exists

When a user hits **"Pay Now"**, many bad things can happen:

- their browser double-clicks
- network retries
- mobile app re-sends the request
- backend restarts mid-transaction
- gateway responds late or fails

If the system is not idempotent, customers will get:

- **double debits**
- **ghost transactions**
- **pending-but-actually-failed transactions**

This system *solves all of that* - elegantly and predictably.

---

# 🏗️ Key Features

✔ Idempotency Keys  
✔ Outbox Pattern  
✔ Background Scheduler (Local Kafka Substitute)  
✔ Retry Logic (3 attempts)  
✔ Dead-Letter Queue (DLQ)  
✔ Async Payment Gateway Simulation  
✔ Swagger UI  
✔ Docker Ready  
✔ GitHub Actions CI

---

# ⚙️ Tech Stack

| Layer | Technology |
|------|------------|
| Backend | Spring Boot 3.5.7 |
| Language | Java 21 |
| Build Tool | Maven |
| Database | PostgreSQL 18 |
| Migrations | Flyway |
| Async | Spring Scheduler |
| JSON | Jackson |
| Infrastructure | Docker & Docker Compose |
| CI | GitHub Actions |

---

# 📂 Project Structure

    .github/workflows/ci.yml
    src/
    ├── main/java/com/payment/route
    │ ├── controller/
    │ ├── service/
    │ ├── scheduler/
    │ ├── model/
    │ ├── repository/
    │ └── dto/
    ├── test/java/com/payment/route/
    │ ├── PaymentServiceConcurrencyTest
    │ ├── OutboxSchedulerIntegrationTest
    │ └── IdempotencyKeyTest
    Dockerfile
    docker-file.yml
    pom.xml
    README.md
    LICENSE


---

# 🔥 How It Works

### 1️⃣ User clicks “Pay Now”
Frontend sends:
- Payment body
- Idempotency-Key: <uuid>
- Client-Id: <merchant-id>

### 2️⃣ System checks idempotency
- Existing → return stored response
- In-progress → 202 PROCESSING
- New → create transaction

### 3️⃣ Transaction + Idempotency Key saved
Stored atomically.

### 4️⃣ Outbox Event created
Represents async payment process request.

### 5️⃣ Scheduler polls outbox events
Runs every 3 seconds.  
Retries failures up to 3 times.

### 6️⃣ If persist failing after 3 failures → moved to DLQ

### 7️⃣ On success
- Payment updated to SUCCESS
- Result cached
- Duplicate requests return instantly

---

# 🏦 High-level Architecture

    ┌───────────────────────────────────────┐
    │     API Gateway POST: ... /payment/   │
    └───────────────────────────────────────┘
                    │
                    ▼
    ┌──────────────────────────┐
    │     PaymentService       │
               -----
    │ - Validates idempotency  │
    │ - Creates transaction    │
    │ - Writes outbox event    │
    └──────────────────────────┘
                    │
                    ▼
    ┌────────────────────────────────┐
    │     PostgreSQL Database        │
                 -----
    │  • payment_transactions        │
    │  • idempotency_keys            │
    │  • outbox                      │
    │  • dead_letter_queue           │
    └────────────────────────────────┘
                    │
                    ▼
    ┌─────────────────────────────┐
    │    OutboxScheduler (Async)  │
                -----
    │ - polls every X seconds     │
    │ - retries 3 times           │
    │ - marks success/failure     │
    │ - moves to DLQ if failed    │
    └─────────────────────────────┘
                    │
                    ▼
    ┌────────────────────────────────┐
    │    GatewaySimulatorService     │
    │                                │
    └────────────────────────────────┘

---

# 🔁 Sequence Flow ([Pay now] -> Success / Failure)

    User Clicks Pay Now
            │ ------------------------------------------------------------
            ▼                                                            |
    POST /api/v1/payments                                                |  IDEMPOTENT
            │                                                            |
            ▼                                                            |
    [Check Idempotency Key]                                              |  PAYMENT
            │                                                            |
            ├─ Key exists + Completed → Return cached result             |
            ├─ Key exists + In Progress → 202 Accepted                   |  ROUTING
            └─ New Key → Create transaction                              |
            ▼                                                            |
    Write Outbox Event                                                   |  SERVICE
            ▼                                                            |
    HTTP 201 Accepted                                                    |
            ▼                                                            |  
    OutboxScheduler picks event                                          |  
                                                                         |  GATEWAY FAILURE
            ▼ --------------------------------------------------------------------------------->
                                                                                               |
            Simulates gateway call                                                             |
                 │                                                                             |
                 ├─ Success → Update transaction → Mark processed     [THIRD PARTY GATEWAY]    |
                 └─ Failure → Retry (max 3)                                                    |
            ▼                                                                                  |
    If still failing → DLQ                                     <-------------------------------


---

# 🧪 Running Tests

Tests include:
- concurrency
- idempotency
- scheduler retry logic
- DLQ flow

---

# 🚢 Run Using Docker

### Build & Start
```bash
    docker compose up --build
```
### Swagger UI
```bash
    http://localhost:8080/swagger-ui.html
```


---

# 📬 API Summary

### POST /api/v1/payments
- Create a new payment.
```bash
 -Headers:
    
    Client-Id: your-client
    Idempotency-Key: UUID
    Content-Type: application/json
    
 -Body: [raw:json]
    
    {
      "sourceAccount": "ACC001",
      "destinationAccount": "ACC002",
      "amount": 650.50,
      "currency": "INR",
      "recipient": "Flipkart",
      "description": "Order #FLP-8832"
    }
```

### GET /api/v1/payments/{id}
- Fetch a transaction.

### GET /api/v1/dead-letter
- List failed events.

---

# 🤝 Contributing

Contributions? PRs? Bug reports?

All welcome!

Just make sure to:
- Follow PR template
- Run all tests
- Keep commit messages clean
- Use conventional commits 🎯

Happy Coding 😊

