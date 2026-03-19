package com.dss.dto;

import lombok.*;

/**
 * Request to train or retrain the ML model for a stock.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ModelTrainRequest {
    private String symbol;
}
