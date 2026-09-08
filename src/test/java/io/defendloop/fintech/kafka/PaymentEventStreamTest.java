package io.defendloop.fintech.kafka;

import io.defendloop.fintech.model.PaymentTransaction;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("FinTech Payment Processing")
@Feature("Kafka Asynchronous Event Stream Testing")
@DisplayName("Kafka Event Stream & Dead Letter Queue (DLQ) Test Suite")
public class PaymentEventStreamTest {

    private KafkaTestHarness kafkaHarness;
    private static final String TOPIC_PAYMENTS = "fintech-payment-events";
    private static final String TOPIC_PAYMENTS_DLQ = "fintech-payment-events-dlq";

    @BeforeEach
    void setup() {
        kafkaHarness = new KafkaTestHarness();
    }

    @AfterEach
    void tearDown() {
        kafkaHarness.reset();
    }

    @Test
    @Story("Asynchronous Event Consumption")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Kafka Producer/Consumer - Payment event publishes and consumes within 2s SLA")
    void shouldPublishAndConsumePaymentEventWithinSla() {
        String txnId = "TXN-KAFKA-" + UUID.randomUUID();
        String idempotencyKey = "IDEMP-KAFKA-" + UUID.randomUUID();

        PaymentTransaction event = PaymentTransaction.builder()
                .transactionId(txnId)
                .idempotencyKey(idempotencyKey)
                .amount(new BigDecimal("1250.75"))
                .currency("USD")
                .sourceAccountId("ACC-FINTECH-001")
                .destinationAccountId("ACC-FINTECH-002")
                .status("PENDING")
                .eventType("PAYMENT_INITIATED")
                .timestamp(Instant.now())
                .build();

        // Publish to Kafka topic
        kafkaHarness.publishEvent(TOPIC_PAYMENTS, event);

        // Await event consumption using Awaitility polling
        PaymentTransaction consumedEvent = kafkaHarness.awaitEvent(
                TOPIC_PAYMENTS, 
                Duration.ofSeconds(2), 
                e -> txnId.equals(e.getTransactionId())
        );

        assertThat(consumedEvent).isNotNull();
        assertThat(consumedEvent.getTransactionId()).isEqualTo(txnId);
        assertThat(consumedEvent.getAmount()).isEqualByComparingTo("1250.75");
        assertThat(consumedEvent.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @Story("Event Stream Idempotency & Deduplication")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Kafka Idempotency - Duplicate events with identical Idempotency-Key are safely dropped")
    void shouldDeduplicateEventsWithIdenticalIdempotencyKeys() {
        String sharedIdempotencyKey = "IDEMP-STREAM-DEDUP-888";

        PaymentTransaction event1 = PaymentTransaction.builder()
                .transactionId("TXN-FIRST-" + UUID.randomUUID())
                .idempotencyKey(sharedIdempotencyKey)
                .amount(new BigDecimal("300.00"))
                .currency("USD")
                .build();

        PaymentTransaction duplicateEvent = PaymentTransaction.builder()
                .transactionId("TXN-SECOND-DUPLICATE")
                .idempotencyKey(sharedIdempotencyKey)
                .amount(new BigDecimal("300.00"))
                .currency("USD")
                .build();

        // Publish both events
        kafkaHarness.publishEvent(TOPIC_PAYMENTS, event1);
        kafkaHarness.publishEvent(TOPIC_PAYMENTS, duplicateEvent);

        List<PaymentTransaction> buffered = kafkaHarness.getBufferedEvents(TOPIC_PAYMENTS);

        // Assert only the first unique event exists in the topic; duplicate was dropped
        assertThat(buffered).hasSize(1);
        assertThat(buffered.get(0).getTransactionId()).isEqualTo(event1.getTransactionId());
    }

    @Test
    @Story("Fault Tolerance & Dead Letter Queue")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Kafka DLQ Routing - Corrupted/Malformed events route to Dead Letter Queue")
    void shouldRouteMalformedPayloadToDeadLetterQueue() {
        // Event missing required amount and transactionId
        PaymentTransaction malformedEvent = new PaymentTransaction();
        malformedEvent.setStatus("MALFORMED");

        kafkaHarness.publishEvent(TOPIC_PAYMENTS, malformedEvent);

        // Verify it was rejected from the primary topic and routed to DLQ
        List<PaymentTransaction> primaryTopicEvents = kafkaHarness.getBufferedEvents(TOPIC_PAYMENTS);
        List<PaymentTransaction> dlqEvents = kafkaHarness.getBufferedEvents(TOPIC_PAYMENTS_DLQ);

        assertThat(primaryTopicEvents).isEmpty();
        assertThat(dlqEvents).hasSize(1);
        assertThat(dlqEvents.get(0).getStatus()).isEqualTo("MALFORMED");
    }
}
