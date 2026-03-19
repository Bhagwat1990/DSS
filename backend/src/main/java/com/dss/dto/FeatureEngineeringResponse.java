package com.dss.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeatureEngineeringResponse {
    private String symbol;
    private int featuresGenerated;
    private String message;
}
