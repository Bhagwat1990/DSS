package com.dss.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ModelTrainResponse {
    private String symbol;
    private int trainingRecords;
    private Double accuracy;
    private String message;
}
