package com.dss.dto;

import lombok.*;

/**
 * Request to trigger feature engineering and label generation.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeatureEngineeringRequest {
    private String symbol;
}
