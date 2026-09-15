# CLAUDE.md: FinTech Test Platform & Distributed Systems Harness

## Overview
`fintech-test-platform-harness` is a Java 17 backend test automation harness for distributed FinTech microservices, validating REST API idempotency, WireMock virtual services, OAuth 2.0 Client Credentials caching, and asynchronous Kafka event streaming with Awaitility.

## Tech Stack
- **Language**: Java 17 LTS
- **Build Tool**: Maven Wrapper (`./mvnw` / `mvnw.cmd`)
- **Testing Tools**: JUnit 5, REST Assured, WireMock, Apache Kafka Client, Awaitility
- **Reporting**: Allure Report, ExtentReports, Jira Xray integration
- **Containers**: Dockerfile, docker-compose (Kafka in KRaft mode)

## Directory Structure
- `src/main/java/io/defendloop/fintech/`: Production test framework utilities (API client, OAuth cache, Kafka test harness, Transaction model).
- `src/test/java/io/defendloop/fintech/`: JUnit 5 suites for API tests, OAuth security tests, and Kafka streaming tests.
- `docker-compose.yml`: Local multi-container topology with Apache Kafka.

## Common Commands
```bash
# Run all tests locally
./mvnw clean test

# Run a specific test class
./mvnw test -Dtest=PaymentApiTest
./mvnw test -Dtest=PaymentEventStreamTest

# Generate and view Allure Report
./mvnw allure:serve

# Containerized execution
docker build -t fintech-test-runner .
docker run --rm fintech-test-runner
```

## Guidelines
- Never use `Thread.sleep()` for asynchronous assertions; always use `Awaitility.await()`.
- Validate idempotency by sending repeated requests with identical `Idempotency-Key` headers.
- When adding Kafka tests, verify DLQ routing for corrupted messages and schema violations.
