package com.dss.service;

import com.dss.client.GrowwApiClient;
import com.dss.dto.groww.GrowwOrderRequest;
import com.dss.dto.groww.GrowwOrderResponse;
import com.dss.dto.groww.GrowwQuoteResponse;
import com.dss.dto.groww.HistoricalCandleResponse;
import com.dss.entity.QuoteHistory;
import com.dss.repository.QuoteHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Business-layer wrapper around {@link GrowwApiClient}.
 * Provides validation, default values, and logging on top of the raw HTTP calls.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrowwService {

    private final GrowwApiClient growwApiClient;
    private final QuoteHistoryRepository quoteHistoryRepository;

    /**
     * Get a live quote for a symbol on the specified exchange.
     */
    public GrowwQuoteResponse getQuote(String exchange, String tradingSymbol) {
        validateNotBlank(tradingSymbol, "tradingSymbol");
        if (exchange == null || exchange.isBlank()) {
            exchange = "NSE";
        }
        log.info("Fetching Groww quote: {}:{}", exchange, tradingSymbol);
        return growwApiClient.getQuote(exchange, tradingSymbol);
    }

    /**
     * Get last traded price for one or more instruments.
     */
    public String getLtp(String segment, String exchangeSymbols) {
        validateNotBlank(exchangeSymbols, "exchangeSymbols");
        if (segment == null || segment.isBlank()) {
            segment = "CASH";
        }
        log.info("Fetching Groww LTP: segment={} symbols={}", segment, exchangeSymbols);
        return growwApiClient.getLtp(segment, exchangeSymbols);
    }

    /**
     * Place an order after basic input validation.
     */
    public GrowwOrderResponse placeOrder(GrowwOrderRequest request) {
        validateNotBlank(request.getTradingSymbol(), "tradingSymbol");
        validateNotBlank(request.getTransactionType(), "transactionType");
        validateNotBlank(request.getOrderType(), "orderType");

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }

        if (request.getExchange() == null || request.getExchange().isBlank()) {
            request.setExchange("NSE");
        }
        if (request.getProduct() == null || request.getProduct().isBlank()) {
            request.setProduct("CNC");
        }
        if (request.getValidity() == null || request.getValidity().isBlank()) {
            request.setValidity("DAY");
        }
        if (request.getSegment() == null || request.getSegment().isBlank()) {
            request.setSegment("CASH");
        }
        if (request.getOrderReferenceId() == null || request.getOrderReferenceId().isBlank()) {
            request.setOrderReferenceId("DSS" + System.currentTimeMillis());
        }

        // LIMIT & SL orders need a price
        String ot = request.getOrderType().toUpperCase();
        if (("LIMIT".equals(ot) || "SL".equals(ot)) && request.getPrice() == null) {
            throw new IllegalArgumentException("price is required for " + ot + " orders");
        }
        if (("SL".equals(ot) || "SL-M".equals(ot)) && request.getTriggerPrice() == null) {
            throw new IllegalArgumentException("triggerPrice is required for " + ot + " orders");
        }

        log.info("Placing Groww order: {} {} {} qty={} price={}",
                request.getExchange(), request.getTransactionType(),
                request.getTradingSymbol(), request.getQuantity(), request.getPrice());

        return growwApiClient.placeOrder(request);
    }

    /**
     * Fetch a live quote and store a snapshot in the quote_history table.
     */
    public QuoteHistory fetchAndStoreQuote(String exchange, String tradingSymbol) {
        GrowwQuoteResponse response = getQuote(exchange, tradingSymbol);
        GrowwQuoteResponse.QuotePayload p = response.getPayload();
        if (p == null) {
            throw new IllegalStateException("Groww returned no payload for " + tradingSymbol);
        }

        QuoteHistory qh = QuoteHistory.builder()
                .symbol(tradingSymbol.toUpperCase())
                .timestamp(LocalDateTime.now())
                .lastPrice(p.getLastPrice())
                .dayChange(p.getDayChange())
                .dayChangePerc(p.getDayChangePerc())
                .volume(p.getVolume() != null ? p.getVolume().longValue() : null)
                .week52High(p.getWeek52High())
                .week52Low(p.getWeek52Low())
                .upperCircuit(p.getUpperCircuitLimit())
                .lowerCircuit(p.getLowerCircuitLimit())
                .build();

        quoteHistoryRepository.save(qh);
        log.info("Stored quote history: {} @ ₹{}", tradingSymbol, p.getLastPrice());
        return qh;
    }

    /**
     * Fetch and store quotes for multiple symbols.
     */
    public List<QuoteHistory> fetchAndStoreQuotes(String exchange, List<String> symbols) {
        List<QuoteHistory> results = new ArrayList<>();
        for (String symbol : symbols) {
            results.add(fetchAndStoreQuote(exchange, symbol));
        }
        return results;
    }

    /**
     * Fetch historical OHLCV candle data for backtesting.
     */
    public HistoricalCandleResponse getHistoricalCandles(
            String exchange, String segment, String growwSymbol,
            String startTime, String endTime, String candleInterval) {
        validateNotBlank(growwSymbol, "growwSymbol");
        validateNotBlank(startTime, "startTime");
        validateNotBlank(endTime, "endTime");
        validateNotBlank(candleInterval, "candleInterval");
        if (exchange == null || exchange.isBlank()) exchange = "NSE";
        if (segment == null || segment.isBlank()) segment = "CASH";
        log.info("Fetching historical candles: {} {} {} [{} → {}] interval={}",
                exchange, segment, growwSymbol, startTime, endTime, candleInterval);
        return growwApiClient.getHistoricalCandles(exchange, segment, growwSymbol, startTime, endTime, candleInterval);
    }

    public List<QuoteHistory> getAllQuoteHistory() {
        return quoteHistoryRepository.findAll();
    }

    public void deleteQuoteHistory(String symbol) {
        validateNotBlank(symbol, "symbol");
        quoteHistoryRepository.deleteBySymbol(symbol.toUpperCase());
        log.info("Deleted quote history for symbol: {}", symbol);
    }

    private void validateNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
