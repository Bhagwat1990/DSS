package com.dss.controller;

import com.dss.client.GrowwApiClient;
import com.dss.dto.groww.GrowwOrderRequest;
import com.dss.dto.groww.GrowwOrderResponse;
import com.dss.dto.groww.GrowwQuoteResponse;
import com.dss.dto.groww.HistoricalCandleResponse;
import com.dss.entity.QuoteHistory;
import com.dss.service.GrowwService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for Groww stock operations — quotes & order placement.
 */
@RestController
@RequestMapping("/api/groww")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GrowwController {

    private final GrowwService growwService;

    /**
     * GET /api/groww/quote?symbol=RELIANCE&exchange=NSE
     *
     * Fetch a live stock quote from Groww.
     */
    @GetMapping("/quote")
    public ResponseEntity<GrowwQuoteResponse> getQuote(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "NSE") String exchange) {
        GrowwQuoteResponse quote = growwService.getQuote(exchange, symbol);
        return ResponseEntity.ok(quote);
    }

    /**
     * GET /api/groww/ltp?segment=CASH&symbols=NSE_RELIANCE,BSE_SENSEX
     *
     * Fetch last traded price for one or more instruments.
     */
    @GetMapping("/ltp")
    public ResponseEntity<String> getLtp(
            @RequestParam(defaultValue = "CASH") String segment,
            @RequestParam String symbols) {
        String ltp = growwService.getLtp(segment, symbols);
        return ResponseEntity.ok(ltp);
    }

    /**
     * POST /api/groww/order
     *
     * Place a buy or sell order.
     *
     * Request body example:
     * <pre>
     * {
     *   "exchange": "NSE",
     *   "trading_symbol": "RELIANCE",
     *   "transaction_type": "BUY",
     *   "order_type": "LIMIT",
     *   "product": "CNC",
     *   "quantity": 10,
     *   "price": 2450.50,
     *   "validity": "DAY",
     *   "segment": "CASH",
     *   "order_reference_id": "DSS12345678"
     * }
     * </pre>
     */
    @PostMapping("/order")
    public ResponseEntity<GrowwOrderResponse> placeOrder(@RequestBody GrowwOrderRequest request) {
        GrowwOrderResponse response = growwService.placeOrder(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/groww/quote/store?exchange=NSE
     * Body: ["HDFC", "WIPRO", "TCS"]
     *
     * Fetch live quotes for each symbol and store them in the quote_history table.
     */
    @PostMapping("/quote/store")
    public ResponseEntity<List<QuoteHistory>> fetchAndStoreQuotes(
            @RequestParam(defaultValue = "NSE") String exchange,
            @RequestBody List<String> symbols) {
        List<QuoteHistory> stored = growwService.fetchAndStoreQuotes(exchange, symbols);
        return ResponseEntity.ok(stored);
    }

    /**
     * GET /api/groww/quote/history
     *
     * Retrieve all stored quote history records.
     */
    @GetMapping("/quote/history")
    public ResponseEntity<List<QuoteHistory>> getQuoteHistory() {
        return ResponseEntity.ok(growwService.getAllQuoteHistory());
    }

    /**
     * DELETE /api/groww/quote/history?symbol=HDFC
     *
     * Delete quote history records for a specific symbol.
     */
    @DeleteMapping("/quote/history")
    public ResponseEntity<Map<String, String>> deleteQuoteHistory(@RequestParam String symbol) {
        growwService.deleteQuoteHistory(symbol);
        return ResponseEntity.ok(Map.of("message", "Deleted quote history for " + symbol));
    }

    /**
     * GET /api/groww/historical/candles?exchange=NSE&segment=CASH&growwSymbol=NSE-WIPRO
     *     &startTime=2025-09-24 10:00:00&endTime=2025-09-24 15:30:00&candleInterval=5minute
     *
     * Fetch historical OHLCV candle data for backtesting.
     */
    @GetMapping("/historical/candles")
    public ResponseEntity<HistoricalCandleResponse> getHistoricalCandles(
            @RequestParam(defaultValue = "NSE") String exchange,
            @RequestParam(defaultValue = "CASH") String segment,
            @RequestParam String growwSymbol,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(defaultValue = "1day") String candleInterval) {
        HistoricalCandleResponse response = growwService.getHistoricalCandles(
                exchange, segment, growwSymbol, startTime, endTime, candleInterval);
        return ResponseEntity.ok(response);
    }

    /**
     * Global handler for Groww API errors → returns 502 Bad Gateway.
     */
    @ExceptionHandler(GrowwApiClient.GrowwApiException.class)
    public ResponseEntity<Map<String, String>> handleGrowwApiError(GrowwApiClient.GrowwApiException ex) {
        return ResponseEntity.status(502).body(Map.of(
                "error", "Groww API Error",
                "message", ex.getMessage()
        ));
    }

    /**
     * Validation errors → 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation Error",
                "message", ex.getMessage()
        ));
    }
}
