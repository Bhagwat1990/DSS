package com.dss.client;

import com.dss.dto.groww.GrowwOrderRequest;
import com.dss.dto.groww.GrowwOrderResponse;
import com.dss.dto.groww.GrowwQuoteResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Low-level Java 21 {@link HttpClient} wrapper for the Groww REST API.
 *
 * <p>Uses virtual-thread executor, HTTP/2, and 15-second timeouts.
 * All public methods throw {@link GrowwApiException} on non-2xx responses.</p>
 *
 * <h3>Endpoints covered</h3>
 * <ul>
 *   <li><b>Get Quote</b> – live price snapshot for a trading symbol</li>
 *   <li><b>Place Order</b> – submit a buy / sell order</li>
 * </ul>
 *
 * <h3>Configuration (application.properties)</h3>
 * <pre>
 * groww.api.base-url=https://groww.in/v1/api
 * groww.api.auth-token=${GROWW_AUTH_TOKEN}
 * </pre>
 */
@Component
@Slf4j
public class GrowwApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String authToken;

    public GrowwApiClient(
            ObjectMapper objectMapper,
            @Value("${groww.api.base-url}") String baseUrl,
            @Value("${groww.api.auth-token}") String authToken) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.authToken = authToken;

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    // ───────────────────────── Get Quote ─────────────────────────

    /**
     * Fetch a live quote for the given {@code exchange:tradingSymbol}.
     *
     * @param exchange      "NSE" or "BSE"
     * @param tradingSymbol e.g. "RELIANCE", "TCS", "INFY"
     * @return parsed quote response
     * @throws GrowwApiException on HTTP or parsing errors
     */
    public GrowwQuoteResponse getQuote(String exchange, String tradingSymbol) {
        String url = String.format(
                "%s/stocks_data/v1/tr_live_prices/exchange/%s/segment/CASH/%s/latest",
                baseUrl, exchange.toUpperCase(), tradingSymbol.toUpperCase());

        log.info("GET quote: {}", url);

        HttpRequest request = newGetRequest(url);
        return execute(request, GrowwQuoteResponse.class);
    }

    /**
     * Convenience overload — defaults to NSE exchange.
     */
    public GrowwQuoteResponse getQuote(String tradingSymbol) {
        return getQuote("NSE", tradingSymbol);
    }

    // ───────────────────────── Place Order ─────────────────────────

    /**
     * Place a buy or sell order.
     *
     * @param orderRequest the order payload
     * @return parsed order response
     * @throws GrowwApiException on HTTP or parsing errors
     */
    public GrowwOrderResponse placeOrder(GrowwOrderRequest orderRequest) {
        String url = baseUrl + "/order/stocks/v2/place";
        log.info("POST order: {} {} {} x{} @ {}",
                orderRequest.getExchange(),
                orderRequest.getTransactionType(),
                orderRequest.getTradingSymbol(),
                orderRequest.getQuantity(),
                orderRequest.getPrice());

        try {
            String body = objectMapper.writeValueAsString(orderRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .header("User-Agent", "DSS-Backend/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            return execute(request, GrowwOrderResponse.class);

        } catch (IOException e) {
            throw new GrowwApiException("Failed to serialize order request", e);
        }
    }

    // ───────────────────────── Internals ─────────────────────────

    private HttpRequest newGetRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .header("User-Agent", "DSS-Backend/1.0")
                .GET()
                .build();
    }

    /**
     * Send the request, assert 2xx, and deserialise the body.
     */
    private <T> T execute(HttpRequest request, Class<T> responseType) {
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("Groww API {} {} → {} ({} chars)",
                    request.method(), request.uri(),
                    response.statusCode(), response.body().length());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Groww API error: {} – {}", response.statusCode(), response.body());
                throw new GrowwApiException(
                        "Groww API returned HTTP " + response.statusCode()
                        + ": " + truncate(response.body(), 500));
            }

            return objectMapper.readValue(response.body(), responseType);

        } catch (IOException e) {
            throw new GrowwApiException("I/O error calling Groww API: " + request.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GrowwApiException("Groww API call interrupted: " + request.uri(), e);
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    // ───────────────────────── Exception ─────────────────────────

    /**
     * Unchecked exception for Groww API failures.
     */
    public static class GrowwApiException extends RuntimeException {
        public GrowwApiException(String message) { super(message); }
        public GrowwApiException(String message, Throwable cause) { super(message, cause); }
    }
}
