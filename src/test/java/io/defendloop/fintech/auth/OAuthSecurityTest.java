package io.defendloop.fintech.auth;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Epic("FinTech Identity & Access Management")
@Feature("OAuth 2.0 Client Credentials Grant")
@DisplayName("OAuth 2.0 Security & Token Caching Test Suite")
public class OAuthSecurityTest {

    private static WireMockServer wireMockServer;
    private OAuthTokenManager tokenManager;
    private String tokenEndpoint;

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
        tokenEndpoint = "http://localhost:" + wireMockServer.port() + "/oauth/v2/token";
        tokenManager = new OAuthTokenManager(tokenEndpoint);
    }

    @Test
    @Story("Token Issuance & Verification")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("OAuth 2.0 - Valid client credentials returns 200 OK and Bearer JWT")
    void testValidClientCredentialsReturnsToken() {
        String mockToken = "jwt-" + UUID.randomUUID();

        stubFor(post(urlEqualTo("/oauth/v2/token"))
                .withRequestBody(containing("grant_type=client_credentials"))
                .withRequestBody(containing("client_id=fintech-service"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"" + mockToken + "\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"scope\":\"payments.write\"}")));

        String token = tokenManager.getValidToken("fintech-service", "sec-123456", "payments.write");

        assertThat(token).isEqualTo(mockToken);
        assertThat(tokenManager.isTokenExpired()).isFalse();
        assertThat(tokenManager.getTokenExpiresAt()).isAfter(Instant.now());

        // Verify endpoint was hit exactly once
        verify(1, postRequestedFor(urlEqualTo("/oauth/v2/token")));
    }

    @Test
    @Story("Token Caching & Reusability")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("OAuth Caching - Subsequent calls reuse cached token without re-hitting identity provider")
    void testTokenCachingReusesTokenWithinValidityWindow() {
        String mockToken = "jwt-cached-" + UUID.randomUUID();

        stubFor(post(urlEqualTo("/oauth/v2/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"" + mockToken + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));

        // First call fetches from IDP
        String token1 = tokenManager.getValidToken("fintech-service", "sec-123", "payments.write");
        // Second call should return cached token immediately
        String token2 = tokenManager.getValidToken("fintech-service", "sec-123", "payments.write");

        assertThat(token1).isEqualTo(mockToken);
        assertThat(token2).isEqualTo(mockToken);

        // Verify IDP was only queried once despite 2 invocations
        verify(1, postRequestedFor(urlEqualTo("/oauth/v2/token")));
    }

    @Test
    @Story("Security Gate Rejection")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("OAuth Security - Invalid client secret throws exception with 401 Unauthorized")
    void testInvalidClientSecretThrowsException() {
        stubFor(post(urlEqualTo("/oauth/v2/token"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"invalid_client\",\"error_description\":\"Bad client credentials\"}")));

        assertThatThrownBy(() -> tokenManager.getValidToken("fintech-service", "wrong-secret", "payments.write"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 401");
    }
}
