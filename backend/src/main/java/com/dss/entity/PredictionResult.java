package com.dss.entity;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Stores prediction results from the ML model and AI advisor suggestions.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PredictionResult {

    private String symbol;
    private LocalDateTime predictionTime;
    private Integer mlPrediction;
    private Double mlConfidence;
    private String signal;
    private String aiSuggestion;
    private String inputSummary;
}
