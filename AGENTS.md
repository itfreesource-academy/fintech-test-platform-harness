# AGENTS.md: FinTech Test Platform & Distributed Systems Harness Context

## 🌟 Executive Summary
`fintech-test-platform-harness` is an enterprise-grade backend test automation platform built in Java 17 LTS to validate distributed financial microservices, asynchronous Apache Kafka event streams, and REST API idempotency with sub-second SLA assertions.

---

## 🏛️ Architecture & Technology Stack
- **Language & Runtime**: Java 17 LTS, Maven Wrapper (`./mvnw` / `mvnw.cmd`)
- **API Automation**: REST Assured 5.4.0
- **Message Broker Testing**: Apache Kafka 3.7.0 client, Awaitility (asynchronous polling assertions)
- **Service Virtualization**: WireMock
- **Unit & Integration Framework**: JUnit 5 (Jupiter)
- **Observability & Reporting**: Allure Report, ExtentReports, Grafana metrics, Jira Xray traceability
- **Container Orchestration**: Docker multi-stage build (`Dockerfile`) and Docker Compose (`docker-compose.yml` with KRaft Kafka)

---

## 📂 Project Organization
```
fintech-test-platform-harness/
├── src/main/java/io/defendloop/fintech/
│   ├── api/PaymentApiClient.java         # REST Assured client with idempotency headers
│   ├── auth/OAuthTokenManager.java       # Thread-safe OAuth 2.0 token caching & auto-renewal
│   ├── kafka/KafkaTestHarness.java       # Awaitility polling consumer & event assertions
│   └── model/PaymentTransaction.java     # Immutable domain builder models
├── src/test/java/io/defendloop/fintech/
│   ├── api/PaymentApiTest.java           # Idempotency, 200 vs 422, WireMock validations
│   ├── auth/OAuthSecurityTest.java       # RFC 6749 Client Credentials flow tests
│   └── kafka/PaymentEventStreamTest.java # Asynchronous Kafka event stream & DLQ assertions
├── Dockerfile                            # Multi-stage CI container
├── docker-compose.yml                    # Local Kafka KRaft + microservice environment
└── pom.xml                               # Dependencies and Surefire/Allure plugins
```

---

## 🚀 Commands & Workflows
```bash
# Compile and run test suite locally
./mvnw clean test

# Generate and view Allure Report
./mvnw allure:serve

# Build and run containerized test runner
docker build -t fintech-test-runner .
docker run --rm fintech-test-runner
```

---

## 🎯 Key Domain Rules & Practices
1. **Idempotency Testing**: Repeat requests with identical `Idempotency-Key` must return `200 OK` and identical transaction response without creating duplicate ledger records.
2. **Asynchronous Polling**: Never use `Thread.sleep()`. Use `Awaitility.await().atMost(2, TimeUnit.SECONDS)` with `pollInterval` to assert message arrival on Kafka topics.
3. **Dead Letter Queue (DLQ)**: Malformed or unparseable JSON messages published to `fintech-payment-events` must be routed to `fintech-payment-events-dlq`.
4. **OAuth 2.0 Caching**: In-memory token cache must invalidate and renew 30 seconds before JWT expiry without causing 401 unauthenticated spikes.
