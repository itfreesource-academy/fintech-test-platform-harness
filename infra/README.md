# FinTech Backend Test Infrastructure (IaC)

This directory contains containerized infrastructure blueprints for executing distributed FinTech test suites in 100% isolated hermetic environments.

---

## 🏛️ Architecture Stack

1. **Apache Kafka (KRaft Mode - Port 9092):**
   * Production-grade Zookeeper-less Kafka broker (`cp-kafka:7.5.0`) orchestrating transaction event partitions.
2. **Kafka Observability UI (Port 8080):**
   * Web UI at `http://localhost:8080` to inspect topics (`fintech-payment-events`), consumer lag, offsets, and JSON payloads.
3. **WireMock Mock Microservice Gateway (Port 8089):**
   * Emulates payment gateway endpoints, OAuth 2.0 token issuance, and account reconciliation.
4. **Containerized Test Runner:**
   * Multi-stage Java 17 test runner executing JUnit 5, REST Assured, and Awaitility assertions.
5. **Kubernetes Cloud-Native Runner (`k8s/`):**
   * Declarative K8s batch `Job` specification for running automated regression gates inside Kubernetes clusters.

---

## 🚀 Running Local Test Infrastructure

```bash
# 1. Start Kafka, Kafka UI, and WireMock
docker compose -f infra/docker-compose.infra.yml up -d kafka-broker kafka-ui wiremock

# 2. View Kafka UI in your browser
open http://localhost:8080

# 3. Run the complete test suite against the containerized infra
docker compose -f infra/docker-compose.infra.yml run --rm test-runner

# 4. Tear down infrastructure
docker compose -f infra/docker-compose.infra.yml down -v
```
