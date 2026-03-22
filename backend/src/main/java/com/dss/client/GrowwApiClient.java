package com.dss.client;

import com.dss.dto.groww.GrowwOrderRequest;
import com.dss.dto.groww.GrowwOrderResponse;
import com.dss.dto.groww.GrowwQuoteResponse;
import com.dss.dto.groww.HistoricalCandleResponse;
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
 * Java 21 {@link HttpClient} wrapper for the official Groww Trading API.
 *
 * <p>Uses virtual-thread executor, HTTP/2, and 15-second timeouts.
 * All public methods throw {@link GrowwApiException} on non-2xx responses.</p>
 *
 * <h3>Endpoints (docs: https://groww.in/trade-api/docs/curl)</h3>
 * <ul>
 *   <li><b>Get Quote</b> – GET /v1/live-data/quote</li>
 *   <li><b>Get LTP</b>   – GET /v1/live-data/ltp</li>
 *   <li><b>Place Order</b> – POST /v1/order/create</li>
 * </ul>
 *
 * <h3>Required headers</h3>
 * <pre>
 * Authorization: Bearer {ACCESS_TOKEN}
 * Accept: application/json
 * X-API-VERSION: 1.0
 * </pre>
 */
@Component
@Slf4j
public class GrowwApiClient {

    private static final String API_VERSION = "1.0";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final GrowwTokenManager tokenManager;

    public GrowwApiClient(
            ObjectMapper objectMapper,
            GrowwTokenManager tokenManager,
            @Value("${groww.api.base-url}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.tokenManager = tokenManager;
        this.baseUrl = baseUrl;

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    // ───────────────────────── Get Quote ─────────────────────────

    /**
     * Fetch a full live quote for the given instrument.
     *
     * @param exchange      "NSE" or "BSE"
     * @param tradingSymbol e.g. "RELIANCE", "TCS", "INFY"
     * @return parsed quote response
     */
    public GrowwQuoteResponse getQuote(String exchange, String tradingSymbol) {
        String url = String.format(
                "%s/live-data/quote?exchange=%s&segment=CASH&trading_symbol=%s",
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

    // ───────────────────────── Get LTP ─────────────────────────

    /**
     * Fetch last traded price for one or more instruments.
     *
     * @param segment         "CASH", "FNO", or "COMMODITY"
     * @param exchangeSymbols comma-separated, e.g. "NSE_RELIANCE,BSE_SENSEX"
     * @return raw JSON string (map of symbol → price)
     */
    public String getLtp(String segment, String exchangeSymbols) {
        String url = String.format(
                "%s/live-data/ltp?segment=%s&exchange_symbols=%s",
                baseUrl, segment.toUpperCase(), exchangeSymbols);

        log.info("GET ltp: {}", url);
        HttpRequest request = newGetRequest(url);
        return executeRaw(request);
    }

    // ───────────────────────── Place Order ─────────────────────────

    /**
     * Place a buy or sell order.
     *
     * @param orderRequest the order payload
     * @return parsed order response
     */
    public GrowwOrderResponse placeOrder(GrowwOrderRequest orderRequest) {
        String url = baseUrl + "/order/create";
        log.info("POST order: {} {} {} qty={} @ {}",
                orderRequest.getExchange(),
                orderRequest.getTransactionType(),
                orderRequest.getTradingSymbol(),
                orderRequest.getQuantity(),
                orderRequest.getPrice());

        try {
            String body = objectMapper.writeValueAsString(orderRequest);

            String token = tokenManager.getAccessToken();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .header("X-API-VERSION", API_VERSION)
                    .header("User-Agent", "DSS-Backend/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            return execute(request, GrowwOrderResponse.class);

        } catch (IOException e) {
            throw new GrowwApiException("Failed to serialize order request", e);
        }
    }

    // ───────────────────────── Historical Candles ─────────────────────────

    /**
     * Fetch historical OHLCV candle data for backtesting.
     *
     * @param exchange       "NSE" or "BSE"
     * @param segment        "CASH" or "FNO"
     * @param growwSymbol    e.g. "NSE-WIPRO", "NSE-NIFTY-30Sep25-FUT"
     * @param startTime      "yyyy-MM-dd HH:mm:ss" or epoch seconds
     * @param endTime        "yyyy-MM-dd HH:mm:ss" or epoch seconds
     * @param candleInterval e.g. "1minute", "5minute", "15minute", "1day"
     * @return parsed candle response
     */
    public HistoricalCandleResponse getHistoricalCandles(
            String exchange, String segment, String growwSymbol,
            String startTime, String endTime, String candleInterval) {

        String url = String.format(
                "%s/historical/candles?exchange=%s&segment=%s&groww_symbol=%s&start_time=%s&end_time=%s&candle_interval=%s",
                baseUrl,
                encode(exchange.toUpperCase()),
                encode(segment.toUpperCase()),
                encode(growwSymbol),
                encode(startTime),
                encode(endTime),
                encode(candleInterval));

        log.info("GET historical candles: {}", url);
        HttpRequest request = newGetRequest(url);
        return execute(request, HistoricalCandleResponse.class);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    // ───────────────────────── Internals ─────────────────────────

    private HttpRequest newGetRequest(String url) {
        String token = tokenManager.getAccessToken();
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .header("X-API-VERSION", API_VERSION)
                .header("User-Agent", "DSS-Backend/1.0")
                .GET()
                .build();
    }

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

    private String executeRaw(HttpRequest request) {
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Groww API error: {} – {}", response.statusCode(), response.body());
                throw new GrowwApiException(
                        "Groww API returned HTTP " + response.statusCode()
                        + ": " + truncate(response.body(), 500));
            }
            return response.body();

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
    public static class GrowwApiException extends RuntimeException {
        public GrowwApiException(String message) { super(message); }
        public GrowwApiException(String message, Throwable cause) { super(message, cause); }
    }
}
