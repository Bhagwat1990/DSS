package com.dss.controller;

import com.dss.dto.AdvisorRequest;
import com.dss.dto.AdvisorResponse;
import com.dss.service.AiAdvisorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for AI-powered trading suggestions.
 * Synthesizes technical indicators, ML predictions, and LLM analysis.
 */
@RestController
@RequestMapping("/api/advisor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdvisorController {

    private final AiAdvisorService aiAdvisorService;

    /**
     * POST /api/advisor/suggest
     * Get AI-powered trading suggestion for a stock.
     *
     * Request body:
     * {
     *   "symbol": "RELIANCE.BSE",
     *   "additionalContext": "News sentiment is Positive, Nifty IT is trending up."
     * }
     *
     * Response:
     * {
     *   "symbol": "RELIANCE.BSE",
     *   "signal": "STRONG_BUY",
     *   "mlConfidence": 0.72,
     *   "mlPrediction": 1,
     *   "aiSuggestion": "Strong Buy Suggestion: RSI at 30 indicates oversold...",
     *   "inputSummary": "Stock: RELIANCE.BSE, Last Close: 2450.50...",
     *   "technicals": { ... },
     *   "timestamp": "2026-03-19T14:30:00"
     * }
     */
    @PostMapping("/suggest")
    public ResponseEntity<AdvisorResponse> getSuggestion(@RequestBody AdvisorRequest request) {
        AdvisorResponse response = aiAdvisorService.getAdvice(request);
        return ResponseEntity.ok(response);
    }
}
