package com.dss.controller;

import com.dss.client.GrowwApiClient;
import com.dss.dto.groww.GrowwOrderRequest;
import com.dss.dto.groww.GrowwOrderResponse;
import com.dss.dto.groww.GrowwQuoteResponse;
import com.dss.service.GrowwService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * POST /api/groww/order
     *
     * Place a buy or sell order.
     *
     * Request body example:
     * <pre>
     * {
     *   "exchange": "NSE",
     *   "tradingSymbol": "RELIANCE",
     *   "transactionType": "BUY",
     *   "orderType": "LIMIT",
     *   "productType": "CNC",
     *   "quantity": 10,
     *   "price": 2450.50
     * }
     * </pre>
     */
    @PostMapping("/order")
    public ResponseEntity<GrowwOrderResponse> placeOrder(@RequestBody GrowwOrderRequest request) {
        GrowwOrderResponse response = growwService.placeOrder(request);
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
