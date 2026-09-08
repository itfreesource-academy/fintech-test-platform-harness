package io.defendloop.fintech.api;

import io.defendloop.fintech.model.PaymentTransaction;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reusable REST Assured API client for the Payment Microservice.
 * Demonstrates clean encapsulation, request specification builders, 
 * dynamic idempotency key injection, and structured logging.
 */
public class PaymentApiClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentApiClient.class);
    private final String baseUrl;
    private String authToken;

    public PaymentApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public PaymentApiClient withAuthToken(String token) {
        this.authToken = token;
        return this;
    }

    private RequestSpecification getBaseSpec() {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON);

        if (authToken != null && !authToken.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + authToken);
        }

        return builder.build();
    }

    /**
     * Submits a payment transaction with an optional Idempotency-Key.
     */
    public Response processPayment(PaymentTransaction transaction, String idempotencyKey) {
        log.info("Sending POST /api/v1/payments: amount={} {}, idempotencyKey={}", 
                transaction.getAmount(), transaction.getCurrency(), idempotencyKey);

        RequestSpecification spec = RestAssured.given().spec(getBaseSpec());
        if (idempotencyKey != null) {
            spec.header("Idempotency-Key", idempotencyKey);
        }

        return spec.body(transaction)
                   .when()
                   .post("/api/v1/payments")
                   .then()
                   .extract()
                   .response();
    }

    /**
     * Fetches a transaction by ID.
     */
    public Response getTransaction(String transactionId) {
        log.info("Sending GET /api/v1/payments/{}", transactionId);
        return RestAssured.given()
                .spec(getBaseSpec())
                .when()
                .get("/api/v1/payments/" + transactionId)
                .then()
                .extract()
                .response();
    }

    /**
     * Reconciles accounts in bulk.
     */
    public Response reconcileBatch(String batchId) {
        log.info("Sending POST /api/v1/accounts/reconcile/{}", batchId);
        return RestAssured.given()
                .spec(getBaseSpec())
                .when()
                .post("/api/v1/accounts/reconcile/" + batchId)
                .then()
                .extract()
                .response();
    }
}
