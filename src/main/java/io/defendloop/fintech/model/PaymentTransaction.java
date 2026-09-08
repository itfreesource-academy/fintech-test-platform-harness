package io.defendloop.fintech.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain model representing a financial transaction event in a distributed payment system.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentTransaction {

    private String transactionId;
    private String idempotencyKey;
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private String status; // PENDING, SETTLED, FAILED, REJECTED
    private String eventType; // PAYMENT_INITIATED, PAYMENT_CLEARED, FRAUD_FLAGGED
    private Instant timestamp;

    public PaymentTransaction() {
    }

    public PaymentTransaction(String transactionId, String idempotencyKey, String sourceAccountId,
                              String destinationAccountId, BigDecimal amount, String currency,
                              String status, String eventType, Instant timestamp) {
        this.transactionId = transactionId;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.eventType = eventType;
        this.timestamp = timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(String sourceAccountId) { this.sourceAccountId = sourceAccountId; }

    public String getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(String destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentTransaction that = (PaymentTransaction) o;
        return Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(idempotencyKey, that.idempotencyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, idempotencyKey);
    }

    @Override
    public String toString() {
        return "PaymentTransaction{" +
                "transactionId='" + transactionId + '\'' +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    public static class Builder {
        private String transactionId;
        private String idempotencyKey;
        private String sourceAccountId;
        private String destinationAccountId;
        private BigDecimal amount;
        private String currency = "USD";
        private String status = "PENDING";
        private String eventType = "PAYMENT_INITIATED";
        private Instant timestamp = Instant.now();

        public Builder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder sourceAccountId(String sourceAccountId) { this.sourceAccountId = sourceAccountId; return this; }
        public Builder destinationAccountId(String destinationAccountId) { this.destinationAccountId = destinationAccountId; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public PaymentTransaction build() {
            return new PaymentTransaction(transactionId, idempotencyKey, sourceAccountId,
                    destinationAccountId, amount, currency, status, eventType, timestamp);
        }
    }
}
