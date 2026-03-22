package com.dss.service;

import com.dss.dto.AdvisorRequest;
import com.dss.dto.AdvisorResponse;
import com.dss.entity.FeatureLabel;
import com.dss.entity.PredictionResult;
import com.dss.entity.StockData;
import com.dss.entity.TechnicalIndicator;
import com.dss.repository.PredictionResultRepository;
import com.dss.repository.StockDataRepository;
import com.dss.repository.TechnicalIndicatorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI Advisor Service
 * Synthesizes technical indicators + ML prediction + optional context
 * using Gemini API to produce a natural-language trading suggestion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAdvisorService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final StockDataRepository stockDataRepository;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final ModelTrainingService modelTrainingService;
    private final PredictionResultRepository predictionResultRepository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    /**
     * Generate a comprehensive AI-powered trading suggestion.
     */
    public AdvisorResponse getAdvice(AdvisorRequest request) {
        String symbol = request.getSymbol();

        // 1. Get latest stock data
        Optional<StockData> latestStockOpt = stockDataRepository.findTopBySymbolOrderByDateDesc(symbol);
        if (latestStockOpt.isEmpty()) {
            return AdvisorResponse.builder()
                    .symbol(symbol)
                    .signal("NO_DATA")
                    .aiSuggestion("No stock data found for " + symbol + ". Please fetch data first using /api/stocks/fetch.")
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        StockData latestStock = latestStockOpt.get();

        // 2. Get latest technical indicators
        Optional<TechnicalIndicator> latestIndicatorOpt = technicalIndicatorRepository
                .findTopBySymbolOrderByDateDesc(symbol);
        TechnicalIndicator ti = latestIndicatorOpt.orElse(null);

        // 3. Get ML prediction
        ModelTrainingService.PredictionOutput mlPrediction = null;
        if (ti != null && modelTrainingService.isModelTrained(symbol)) {
            FeatureLabel currentFeatures = FeatureLabel.builder()
                    .rsi(ti.getRsi())
                    .smaShortRatio(ti.getSmaShort() != null && ti.getSmaShort() != 0
                            ? latestStock.getClose() / ti.getSmaShort() : null)
                    .smaLongRatio(ti.getSmaLong() != null && ti.getSmaLong() != 0
                            ? latestStock.getClose() / ti.getSmaLong() : null)
                    .macd(ti.getMacd())
                    .macdSignal(ti.getMacdSignal())
                    .bollingerPosition(ti.getBollingerUpper() != null && ti.getBollingerLower() != null
                            ? (latestStock.getClose() - ti.getBollingerLower()) /
                              (ti.getBollingerUpper() - ti.getBollingerLower()) : null)
                    .label(0) // placeholder
                    .build();
            mlPrediction = modelTrainingService.predictForSymbol(symbol, currentFeatures);
        }

        // 4. Build input summary for LLM
        String inputSummary = buildInputSummary(symbol, latestStock, ti, mlPrediction,
                request.getAdditionalContext());

        // 5. Determine signal from ML model
        String signal = determineSignal(ti, mlPrediction);

        // 6. Call Gemini API for natural language suggestion
        String aiSuggestion = callGeminiApi(inputSummary);

        // 7. Build technical snapshot
        AdvisorResponse.TechnicalSnapshot snapshot = null;
        if (ti != null) {
            snapshot = AdvisorResponse.TechnicalSnapshot.builder()
                    .rsi(ti.getRsi())
                    .smaShort(ti.getSmaShort())
                    .smaLong(ti.getSmaLong())
                    .macd(ti.getMacd())
                    .macdSignal(ti.getMacdSignal())
                    .bollingerUpper(ti.getBollingerUpper())
                    .bollingerLower(ti.getBollingerLower())
                    .atr(ti.getAtr())
                    .lastClose(latestStock.getClose())
                    .build();
        }

        // 8. Store prediction result
        PredictionResult result = PredictionResult.builder()
                .symbol(symbol)
                .predictionTime(LocalDateTime.now())
                .mlPrediction(mlPrediction != null ? mlPrediction.prediction : null)
                .mlConfidence(mlPrediction != null ? mlPrediction.confidence : null)
                .signal(signal)
                .aiSuggestion(aiSuggestion)
                .inputSummary(inputSummary)
                .build();
        predictionResultRepository.save(result);

        return AdvisorResponse.builder()
                .symbol(symbol)
                .signal(signal)
                .mlPrediction(mlPrediction != null ? mlPrediction.prediction : null)
                .mlConfidence(mlPrediction != null ? mlPrediction.confidence : null)
                .aiSuggestion(aiSuggestion)
                .inputSummary(inputSummary)
                .technicals(snapshot)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String buildInputSummary(String symbol, StockData latestStock, TechnicalIndicator ti,
                                      ModelTrainingService.PredictionOutput mlPrediction,
                                      String additionalContext) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Stock: %s, Last Close: %.2f, Date: %s. ",
                symbol, latestStock.getClose(), latestStock.getDate()));

        if (ti != null) {
            if (ti.getRsi() != null) {
                String rsiZone = ti.getRsi() < 30 ? " (oversold)" : ti.getRsi() > 70 ? " (overbought)" : " (neutral)";
                sb.append(String.format("RSI: %.2f%s. ", ti.getRsi(), rsiZone));
            }
            if (ti.getMacd() != null) {
                sb.append(String.format("MACD: %.4f, Signal: %.4f. ", ti.getMacd(),
                        ti.getMacdSignal() != null ? ti.getMacdSignal() : 0.0));
            }
            if (ti.getSmaShort() != null && ti.getSmaLong() != null) {
                String trend = ti.getSmaShort() > ti.getSmaLong() ? "uptrend" : "downtrend";
                sb.append(String.format("SMA20: %.2f, SMA50: %.2f (%s). ",
                        ti.getSmaShort(), ti.getSmaLong(), trend));
            }
            if (ti.getBollingerUpper() != null) {
                sb.append(String.format("Bollinger Bands: [%.2f - %.2f]. ",
                        ti.getBollingerLower(), ti.getBollingerUpper()));
            }
            if (ti.getAtr() != null) {
                sb.append(String.format("ATR: %.4f. ", ti.getAtr()));
            }
        }

        if (mlPrediction != null && mlPrediction.modelAvailable) {
            sb.append(String.format("ML Prediction: %s (confidence: %.1f%%). ",
                    mlPrediction.prediction == 1 ? "UP" : "DOWN", mlPrediction.confidence * 100));
        }

        if (additionalContext != null && !additionalContext.isBlank()) {
            sb.append("Additional context: ").append(additionalContext).append(". ");
        }

        return sb.toString();
    }

    private String determineSignal(TechnicalIndicator ti, ModelTrainingService.PredictionOutput mlPrediction) {
        int bullishSignals = 0;
        int totalSignals = 0;

        if (ti != null) {
            if (ti.getRsi() != null) {
                totalSignals++;
                if (ti.getRsi() < 40) bullishSignals++; // oversold = bullish reversal
            }
            if (ti.getMacd() != null && ti.getMacdSignal() != null) {
                totalSignals++;
                if (ti.getMacd() > ti.getMacdSignal()) bullishSignals++; // bullish crossover
            }
            if (ti.getSmaShort() != null && ti.getSmaLong() != null) {
                totalSignals++;
                if (ti.getSmaShort() > ti.getSmaLong()) bullishSignals++; // uptrend
            }
        }

        if (mlPrediction != null && mlPrediction.modelAvailable) {
            totalSignals += 2; // weight ML more
            if (mlPrediction.prediction == 1) bullishSignals += 2;
        }

        if (totalSignals == 0) return "HOLD";

        double ratio = (double) bullishSignals / totalSignals;
        if (ratio >= 0.8) return "STRONG_BUY";
        if (ratio >= 0.6) return "BUY";
        if (ratio >= 0.4) return "HOLD";
        if (ratio >= 0.2) return "SELL";
        return "STRONG_SELL";
    }

    private String callGeminiApi(String inputSummary) {
        String prompt = String.format(
                "You are a professional stock market analyst AI. Analyze the following stock data and " +
                "provide a concise trading suggestion with reasoning. Be specific about the signals.\n\n" +
                "Data: %s\n\n" +
                "Provide your analysis in this format:\n" +
                "1. Signal (Strong Buy / Buy / Hold / Sell / Strong Sell)\n" +
                "2. Key technical observations (2-3 points)\n" +
                "3. Brief reasoning for the suggestion\n" +
                "Keep it under 150 words.", inputSummary);

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String requestBody = objectMapper.writeValueAsString(Map.of(
                        "contents", List.of(Map.of(
                                "parts", List.of(Map.of("text", prompt))
                        ))
                ));

                String response = webClient.post()
                        .uri(geminiApiUrl + "?key=" + geminiApiKey)
                        .header("Content-Type", "application/json")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JsonNode root = objectMapper.readTree(response);
                JsonNode candidates = root.get("candidates");
                if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode content = candidates.get(0).get("content");
                    if (content != null) {
                        JsonNode parts = content.get("parts");
                        if (parts != null && parts.isArray() && !parts.isEmpty()) {
                            return parts.get(0).get("text").asText();
                        }
                    }
                }

                log.warn("Unexpected Gemini API response structure: {}", response);
                return "AI analysis unavailable. Raw response: " + response;

            } catch (Exception e) {
                String msg = e.getMessage();
                boolean rateLimited = msg != null && (msg.contains("429") || msg.contains("Too Many Requests")
                        || msg.contains("RESOURCE_EXHAUSTED"));
                if (rateLimited && attempt < maxRetries) {
                    log.warn("Gemini API rate limited (attempt {}/{}), retrying after {}s...", attempt, maxRetries, attempt * 5);
                    try { Thread.sleep(attempt * 5000L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                log.error("Error calling Gemini API (attempt {}/{})", attempt, maxRetries, e);
                if (rateLimited) {
                    return "AI analysis temporarily unavailable: API rate limit exceeded. Please try again in a minute.";
                }
                return "AI analysis temporarily unavailable: " + msg;
            }
        }
        return "AI analysis temporarily unavailable: max retries exceeded.";
    }
}
