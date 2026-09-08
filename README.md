# FinTech Test Platform & Distributed Systems Harness

[![FinTech Quality Gate & Test CI](https://github.com/itfreesource-academy/fintech-test-platform-harness/actions/workflows/ci.yml/badge.svg)](https://github.com/itfreesource-academy/fintech-test-platform-harness/actions)
![Java 17](https://img.shields.io/badge/Java-17%20LTS-orange?logo=openjdk)
![REST Assured](https://img.shields.io/badge/REST%20Assured-5.4.0-brightgreen)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7.0-black?logo=apachekafka)
![Docker](https://img.shields.io/badge/Docker-Containerized-blue?logo=docker)
![JUnit 5](https://img.shields.io/badge/JUnit-5-red?logo=junit5)
![Allure](https://img.shields.io/badge/Allure-Reporting-blueviolet)

An enterprise-grade test automation platform and testing harness designed for **distributed FinTech microservices**, **asynchronous Kafka event streams**, and **containerized CI/CD pipelines**. 

Engineered by **[Vishal Prajapati](https://defendloop.io)** (*Senior Automation & Tools Development Engineer*) to showcase production patterns for high-scale, mission-critical financial systems.

---

## 🏛️ Architecture Overview

```mermaid
flowchart TD
    subgraph CI_CD ["CI/CD Pipeline (GitHub Actions)"]
        PR["Git Pull Request / Commit"] --> GA["GitHub Actions Runner (Ubuntu)"]
        GA --> DOCKER["Dockerized Container Environment"]
    end

    subgraph TEST_HARNESS ["Core Test Automation Harness"]
        DOCKER --> HARNESS["Test Runner (JUnit 5 + Allure)"]
        HARNESS --> API_CLIENT["REST Assured Payment Client"]
        HARNESS --> KAFKA_CLIENT["Kafka Event Harness (Awaitility)"]
    end

    subgraph SYSTEM_UNDER_TEST ["Distributed FinTech Services"]
        API_CLIENT -->|HTTP / JSON + Idempotency-Key| MOCK_API["Payment Microservice (WireMock)"]
        KAFKA_CLIENT -->|Publish Transaction Events| KAFKA_TOPIC["fintech-payment-events"]
        KAFKA_TOPIC -->|Deduplication & Order Match| CONSUMER["Payment Processing Engine"]
        KAFKA_TOPIC -->|Malformed Payload Detection| DLQ["fintech-payment-events-dlq"]
    end

    subgraph OBSERVABILITY ["Observability & Reporting"]
        HARNESS --> ALLURE_REPORT["Allure Test Reports"]
        HARNESS --> SUREFIRE["JUnit XML Artifacts"]
    end
```

---

## 🎯 Core Capabilities Demonstrated

### 1. Financial Idempotency & Deduplication Testing
* **The Problem:** In high-volume financial platforms (processing $250B+ annually), network timeouts cause clients to retry requests, risking double-charging users.
* **The Harness Solution:** Test cases validate that duplicate requests transmitting the same `Idempotency-Key` are recognized, returning the original transaction with status `200 OK` without creating duplicate ledger records.

### 2. Asynchronous Kafka Stream Assertions with Awaitility
* **The Problem:** Event-driven architectures are asynchronous; traditional `Thread.sleep()` creates brittle, slow, and flaky tests.
* **The Harness Solution:** Custom `KafkaTestHarness` uses **Awaitility** polling to asynchronously assert event arrival, payload integrity, and schema compliance within a strict 2-second SLA.

### 3. Dead Letter Queue (DLQ) & Fault Tolerance
* Validates that corrupted, unparseable, or malformed transaction events are intercepted and routed to `fintech-payment-events-dlq` to prevent consumer group blocking.

### 4. OAuth 2.0 Client Credentials Token Management
* Built-in `OAuthTokenManager` implements RFC 6749 client credentials flow with thread-safe access token caching, automatic expiration detection (with 30s buffer), and automated renewal against WireMock identity providers.

### 5. Shift-Left API Security & Negative Testing
* Validates negative business logic (e.g., `422 Unprocessable Entity` on insufficient funds) and intercepts security regressions (`401 Unauthorized` on missing/expired Bearer tokens).

### 6. Multi-Service Container Orchestration
* Features both a multi-stage `Dockerfile` and a `docker-compose.yml` defining an isolated multi-service test environment (Kafka Broker + KRaft, mock microservices, and test runner container).

---

## 📂 Project Structure

```
fintech-test-platform-harness/
├── .github/
│   └── workflows/
│       └── ci.yml                 # Automated GitHub Actions CI pipeline
├── .mvn/wrapper/                  # Maven Wrapper binaries
├── src/
│   ├── main/java/io/defendloop/fintech/
│   │   ├── api/
│   │   │   └── PaymentApiClient.java     # Reusable REST Assured client wrapper
│   │   ├── auth/
│   │   │   └── OAuthTokenManager.java    # Thread-safe OAuth 2.0 token manager & cache
│   │   ├── kafka/
│   │   │   └── KafkaTestHarness.java     # Reusable Kafka event testing utility
│   │   └── model/
│   │       └── PaymentTransaction.java   # Domain model & Builder pattern
│   └── test/java/io/defendloop/fintech/
│       ├── api/
│       │   └── PaymentApiTest.java       # WireMock + REST Assured test suite
│       ├── auth/
│       │   └── OAuthSecurityTest.java    # OAuth 2.0 Client Credentials & caching suite
│       └── kafka/
│           └── PaymentEventStreamTest.java # Kafka event stream & DLQ test suite
├── Dockerfile                     # Multi-stage containerized test runner
├── docker-compose.yml             # Local Kafka + Service orchestration
├── mvnw / mvnw.cmd                # Cross-platform Maven Wrapper scripts
├── pom.xml                        # Maven dependency & plugin configuration
└── README.md
```

---

## 🚀 Running the Test Harness

### Option A: Local Execution via Maven
```bash
# Clone the repository
git clone https://github.com/itfreesource-academy/fintech-test-platform-harness.git
cd fintech-test-platform-harness

# Run all test suites
mvn clean test

# View Allure Test Report
mvn allure:serve
```

### Option B: Containerized Execution via Docker
```bash
# Build and run the test container
docker build -t fintech-test-runner .
docker run --rm fintech-test-runner
```

---

## 👤 Author
* **Vishal Prajapati** — *Senior Associate: Test Automation & Tools Development Engineer*
* **Portfolio:** [defendloop.io](https://defendloop.io)
* **LinkedIn:** [linkedin.com/in/vishalprajapati2k25](https://www.linkedin.com/in/vishalprajapati2k25)
* **Education & Community:** Founder of [ITFreeSource Academy](https://academy.itfreesource.com)
