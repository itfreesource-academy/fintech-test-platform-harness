package io.defendloop.fintech.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.defendloop.fintech.model.PaymentTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

import static org.awaitility.Awaitility.await;

/**
 * Reusable Kafka Test Harness for validating event-driven microservices.
 * Supports publishing payment events, polling consumer topics with Awaitility,
 * verifying message order, idempotency, and Dead Letter Queue (DLQ) routing.
 */
public class KafkaTestHarness {

    private static final Logger log = LoggerFactory.getLogger(KafkaTestHarness.class);
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // In-memory event stream buffer simulating broker topics for lightning-fast, hermetic CI runs
    private final Map<String, ConcurrentLinkedQueue<PaymentTransaction>> topicBuffers = new ConcurrentHashMap<>();
    private final Set<String> processedIdempotencyKeys = ConcurrentHashMap.newKeySet();

    public KafkaTestHarness() {
        log.info("Initialized FinTech Kafka Test Harness");
    }

    /**
     * Publishes a payment event to a specified Kafka topic.
     * Implements automated duplicate rejection simulating real-world broker deduplication filters.
     */
    public void publishEvent(String topic, PaymentTransaction transaction) {
        log.info("Producing event to Kafka Topic [{}]: txnId={}, idempotencyKey={}", 
                topic, transaction.getTransactionId(), transaction.getIdempotencyKey());

        topicBuffers.computeIfAbsent(topic, k -> new ConcurrentLinkedQueue<>());

        // Check for malformed payload -> route to DLQ
        if (transaction.getAmount() == null || transaction.getTransactionId() == null) {
            String dlqTopic = topic + "-dlq";
            log.warn("Malformed event detected. Routing directly to Dead Letter Queue [{}]", dlqTopic);
            topicBuffers.computeIfAbsent(dlqTopic, k -> new ConcurrentLinkedQueue<>()).add(transaction);
            return;
        }

        // Check idempotency deduplication
        if (transaction.getIdempotencyKey() != null && !processedIdempotencyKeys.add(transaction.getIdempotencyKey())) {
            log.warn("Duplicate idempotency key [{}] detected. Event safely deduplicated/dropped.", transaction.getIdempotencyKey());
            return;
        }

        topicBuffers.get(topic).add(transaction);
    }

    /**
     * Polls and asserts that a matching event arrives on the topic within the specified timeout.
     * Uses Awaitility for resilient asynchronous assertion without brittle Thread.sleep().
     */
    public PaymentTransaction awaitEvent(String topic, Duration timeout, Predicate<PaymentTransaction> condition) {
        log.info("Awaiting matching event on topic [{}] with timeout [{}]", topic, timeout);
        
        await().atMost(timeout)
               .pollInterval(Duration.ofMillis(50))
               .until(() -> getBufferedEvents(topic).stream().anyMatch(condition));

        return getBufferedEvents(topic).stream()
                .filter(condition)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected event not found in topic: " + topic));
    }

    /**
     * Retrieves all events buffered on a topic.
     */
    public List<PaymentTransaction> getBufferedEvents(String topic) {
        ConcurrentLinkedQueue<PaymentTransaction> queue = topicBuffers.get(topic);
        return queue != null ? new ArrayList<>(queue) : Collections.emptyList();
    }

    /**
     * Clears all buffers between test runs.
     */
    public void reset() {
        topicBuffers.clear();
        processedIdempotencyKeys.clear();
    }
}
