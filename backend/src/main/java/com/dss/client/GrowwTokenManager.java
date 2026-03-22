package com.dss.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages Groww API access tokens using the API Key + Secret flow.
 *
 * <p>Generates a SHA-256 checksum from {@code secret + timestamp}, then calls
 * {@code POST /v1/token/api/access} to obtain a fresh access token.
 * Tokens are cached and auto-refreshed when expired.</p>
 *
 * @see <a href="https://groww.in/trade-api/docs/curl">Groww API Docs</a>
 */
@Component
@Slf4j
public class GrowwTokenManager {

    private static final String TOKEN_ENDPOINT = "/token/api/access";
    private static final Duration TOKEN_TTL = Duration.ofHours(20); // refresh well before 6AM expiry

    private final String baseUrl;
    private final String apiKey;
    private final String apiSecret;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public GrowwTokenManager(
            ObjectMapper objectMapper,
            @Value("${groww.api.base-url}") String baseUrl,
            @Value("${groww.api.api-key}") String apiKey,
            @Value("${groww.api.secret}") String apiSecret) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Returns a valid access token, refreshing it if expired.
     */
    public String getAccessToken() {
        if (isTokenValid()) {
            return cachedAccessToken;
        }
        lock.lock();
        try {
            // Double-check after acquiring lock
            if (isTokenValid()) {
                return cachedAccessToken;
            }
            refreshToken();
            return cachedAccessToken;
        } finally {
            lock.unlock();
        }
    }

    private boolean isTokenValid() {
        return cachedAccessToken != null && Instant.now().isBefore(tokenExpiresAt);
    }

    private void refreshToken() {
        try {
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String checksum = generateChecksum(apiSecret, timestamp);

            String body = objectMapper.writeValueAsString(new TokenRequest("approval", checksum, timestamp));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + TOKEN_ENDPOINT))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            log.info("Requesting new Groww access token...");

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Failed to obtain Groww access token: {} – {}", response.statusCode(), response.body());
                throw new GrowwApiClient.GrowwApiException(
                        "Token refresh failed (HTTP " + response.statusCode() + "): " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            // Response might be wrapped in "payload" or be top-level
            JsonNode payload = root.has("payload") ? root.get("payload") : root;

            String token = payload.get("token").asText();
            cachedAccessToken = token;
            tokenExpiresAt = Instant.now().plus(TOKEN_TTL);

            log.info("Groww access token obtained successfully (expires ~{})", tokenExpiresAt);

        } catch (GrowwApiClient.GrowwApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GrowwApiClient.GrowwApiException("Failed to refresh Groww access token", e);
        }
    }

    /**
     * SHA-256 hash of (secret + timestamp) as per Groww docs.
     */
    static String generateChecksum(String secret, String timestamp) {
        try {
            String input = secret + timestamp;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Simple record for the token request body.
     */
    private record TokenRequest(String key_type, String checksum, String timestamp) {}
}
