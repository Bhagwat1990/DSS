package com.dss.controller;

import com.dss.dto.StockDataRequest;
import com.dss.dto.StockDataResponse;
import com.dss.service.StockDataService;
import com.dss.service.TechnicalIndicatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for stock data operations (Phase 1: Data Pipeline).
 */
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StockDataController {

    private final StockDataService stockDataService;
    private final TechnicalIndicatorService technicalIndicatorService;

    /**
     * POST /api/stocks/fetch
     * Fetch stock data from Alpha Vantage and store in database.
     *
     * Request body:
     * {
     *   "symbol": "RELIANCE.BSE",
     *   "outputSize": "compact"
     * }
     */
    @PostMapping("/fetch")
    public ResponseEntity<StockDataResponse> fetchStockData(@RequestBody StockDataRequest request) {
        StockDataResponse response = stockDataService.fetchAndStoreStockData(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/stocks/{symbol}
     * Retrieve stored stock data for a symbol.
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<StockDataResponse> getStockData(@PathVariable String symbol) {
        StockDataResponse response = stockDataService.getStoredStockData(symbol);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/stocks/{symbol}/indicators
     * Calculate and store technical indicators for a symbol.
     */
    @PostMapping("/{symbol}/indicators")
    public ResponseEntity<Map<String, Object>> calculateIndicators(@PathVariable String symbol) {
        int count = technicalIndicatorService.calculateAndStoreIndicators(symbol);
        return ResponseEntity.ok(Map.of(
                "symbol", symbol,
                "indicatorsCalculated", count,
                "message", count > 0
                        ? "Successfully calculated " + count + " indicator records"
                        : "Insufficient data. Fetch stock data first (need at least 50 data points)."
        ));
    }
}
