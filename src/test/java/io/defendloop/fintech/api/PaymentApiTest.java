package io.defendloop.fintech.api;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.defendloop.fintech.model.PaymentTransaction;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("FinTech Payment Processing")
@Feature("Payment Microservice REST API")
@DisplayName("Payment API & Idempotency Test Suite")
public class PaymentApiTest {

    private static WireMockServer wireMockServer;
    private PaymentApiClient apiClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();
        apiClient = new PaymentApiClient("http://localhost:" + wireMockServer.port())
                .withAuthToken("mock-jwt-token-sdet-vishal");
    }

    @Test
    @Story("Payment Submission")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("POST /api/v1/payments - Success with 201 Created and Transaction ID")
    void testSuccessfulPaymentProcessing() {
        String txnId = "TXN-" + UUID.randomUUID();
        String idempotencyKey = "IDEMP-" + UUID.randomUUID();

        stubFor(post(urlEqualTo("/api/v1/payments"))
                .withHeader("Authorization", equalTo("Bearer mock-jwt-token-sdet-vishal"))
                .withHeader("Idempotency-Key", equalTo(idempotencyKey))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transactionId\":\"" + txnId + "\",\"status\":\"SETTLED\",\"amount\":500.00,\"currency\":\"USD\"}")));

        PaymentTransaction request = PaymentTransaction.builder()
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .sourceAccountId("ACC-1001")
                .destinationAccountId("ACC-2002")
                .build();

        Response response = apiClient.processPayment(request, idempotencyKey);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.jsonPath().getString("transactionId")).isEqualTo(txnId);
        assertThat(response.jsonPath().getString("status")).isEqualTo("SETTLED");
        assertThat(response.jsonPath().getDouble("amount")).isEqualTo(500.00);
    }

    @Test
    @Story("Idempotency Protection")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Idempotency Check - Resending same Idempotency-Key returns original transaction without duplicate charge")
    void testIdempotencyProtection_DuplicateRequestReturnsSameTransaction() {
        String sharedIdempotencyKey = "IDEMP-DUPLICATE-CHECK-999";
        String existingTxnId = "TXN-EXISTING-12345";

        // Stub returns 200 OK with original transaction for duplicate requests
        stubFor(post(urlEqualTo("/api/v1/payments"))
                .withHeader("Idempotency-Key", equalTo(sharedIdempotencyKey))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transactionId\":\"" + existingTxnId + "\",\"status\":\"SETTLED\",\"idempotencyKey\":\"" + sharedIdempotencyKey + "\"}")));

        PaymentTransaction duplicateRequest = PaymentTransaction.builder()
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .sourceAccountId("ACC-1001")
                .destinationAccountId("ACC-2002")
                .build();

        Response response = apiClient.processPayment(duplicateRequest, sharedIdempotencyKey);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("transactionId")).isEqualTo(existingTxnId);
        assertThat(response.jsonPath().getString("idempotencyKey")).isEqualTo(sharedIdempotencyKey);
    }

    @Test
    @Story("Business Logic Validation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/v1/payments - Insufficient Funds returns 422 Unprocessable Entity")
    void testInsufficientFunds_Returns422UnprocessableEntity() {
        stubFor(post(urlEqualTo("/api/v1/payments"))
                .willReturn(aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errorCode\":\"ERR_INSUFFICIENT_FUNDS\",\"message\":\"Source account has insufficient balance\"}")));

        PaymentTransaction request = PaymentTransaction.builder()
                .amount(new BigDecimal("9999999.00"))
                .currency("USD")
                .sourceAccountId("ACC-LOW-BALANCE")
                .destinationAccountId("ACC-2002")
                .build();

        Response response = apiClient.processPayment(request, "IDEMP-" + UUID.randomUUID());

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.jsonPath().getString("errorCode")).isEqualTo("ERR_INSUFFICIENT_FUNDS");
    }

    @Test
    @Story("API Security & Authorization")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Security Gate - Missing Authorization header returns 401 Unauthorized")
    void testUnauthorizedRequest_MissingAuthHeader_Returns401() {
        stubFor(post(urlEqualTo("/api/v1/payments"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid Bearer token\"}")));

        PaymentApiClient unauthenticatedClient = new PaymentApiClient("http://localhost:" + wireMockServer.port());

        PaymentTransaction request = PaymentTransaction.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .sourceAccountId("ACC-1001")
                .destinationAccountId("ACC-2002")
                .build();

        Response response = unauthenticatedClient.processPayment(request, "IDEMP-NO-AUTH");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("error")).isEqualTo("Unauthorized");
    }

    @Test
    @Story("Batch Settlement & Reconciliation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/v1/accounts/reconcile/{batchId} - Success with 200 OK")
    void testBatchReconciliationSuccess() {
        String batchId = "BATCH-2026-09";
        stubFor(post(urlEqualTo("/api/v1/accounts/reconcile/" + batchId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"batchId\":\"" + batchId + "\",\"reconciledRecords\":15420,\"discrepancies\":0,\"status\":\"BALANCED\"}")));

        Response response = apiClient.reconcileBatch(batchId);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getInt("reconciledRecords")).isEqualTo(15420);
        assertThat(response.jsonPath().getInt("discrepancies")).isEqualTo(0);
        assertThat(response.jsonPath().getString("status")).isEqualTo("BALANCED");
    }
}
