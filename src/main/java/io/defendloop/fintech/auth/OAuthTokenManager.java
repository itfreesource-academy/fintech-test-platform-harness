package io.defendloop.fintech.auth;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;

/**
 * Thread-safe OAuth 2.0 Token Manager.
 * Implements token caching, expiration detection, and automated renewal
 * adhering to OAuth 2.0 RFC 6749 Client Credentials Grant.
 */
public class OAuthTokenManager {

    private static final Logger log = LoggerFactory.getLogger(OAuthTokenManager.class);
    private static final int EXPIRY_BUFFER_SECONDS = 30;

    private final String tokenEndpointUrl;
    private String cachedAccessToken;
    private Instant tokenExpiresAt;

    public OAuthTokenManager(String tokenEndpointUrl) {
        this.tokenEndpointUrl = Objects.requireNonNull(tokenEndpointUrl, "tokenEndpointUrl cannot be null");
    }

    /**
     * Retrieves a valid cached access token, automatically refreshing from the
     * identity provider if the token is missing or nearing expiration.
     */
    public synchronized String getValidToken(String clientId, String clientSecret, String scope) {
        if (isTokenExpired()) {
            log.info("OAuth token expired or not present. Requesting new access token for client: {}", clientId);
            refreshToken(clientId, clientSecret, scope);
        }
        return cachedAccessToken;
    }

    /**
     * Executes the OAuth 2.0 Client Credentials Grant request.
     */
    private void refreshToken(String clientId, String clientSecret, String scope) {
        Response response = RestAssured.given()
                .contentType(ContentType.URLENC)
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .formParam("scope", scope)
                .when()
                .post(tokenEndpointUrl);

        if (response.statusCode() != 200) {
            log.error("Failed to acquire OAuth token. Status: {}, Body: {}", response.statusCode(), response.body().asString());
            throw new IllegalStateException("OAuth 2.0 token request failed with HTTP " + response.statusCode());
        }

        this.cachedAccessToken = response.jsonPath().getString("access_token");
        int expiresIn = response.jsonPath().getInt("expires_in");
        this.tokenExpiresAt = Instant.now().plusSeconds(expiresIn);

        log.info("Acquired fresh OAuth token (expires in {}s)", expiresIn);
    }

    /**
     * Checks if current cached token is null or within the safety buffer window.
     */
    public boolean isTokenExpired() {
        if (cachedAccessToken == null || tokenExpiresAt == null) {
            return true;
        }
        return Instant.now().isAfter(tokenExpiresAt.minusSeconds(EXPIRY_BUFFER_SECONDS));
    }

    /**
     * Invalidate cache to force token renewal on next request.
     */
    public synchronized void invalidateCache() {
        this.cachedAccessToken = null;
        this.tokenExpiresAt = null;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }
}
