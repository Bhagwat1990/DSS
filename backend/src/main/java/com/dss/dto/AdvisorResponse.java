package com.dss.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdvisorResponse {
    private String symbol;
    private String signal;             // STRONG_BUY, BUY, HOLD, SELL, STRONG_SELL
    private Double mlConfidence;       // ML model confidence
    private Integer mlPrediction;      // 1 = up, 0 = down
    private String aiSuggestion;       // LLM-generated explanation
    private String inputSummary;       // What data was fed to the LLM
    private TechnicalSnapshot technicals;
    private LocalDateTime timestamp;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TechnicalSnapshot {
        private Double rsi;
        private Double smaShort;
        private Double smaLong;
        private Double macd;
        private Double macdSignal;
        private Double bollingerUpper;
        private Double bollingerLower;
        private Double atr;
        private Double lastClose;
    }
}
