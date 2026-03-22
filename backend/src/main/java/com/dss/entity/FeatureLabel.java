package com.dss.entity;

import lombok.*;
import java.time.LocalDate;

/**
 * Stores computed features and the target label for ML training.
 * Label: 1 = price goes up next day, 0 = price goes down next day.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeatureLabel {

    private String symbol;
    private LocalDate date;

    // Features
    private Double rsi;
    private Double smaShortRatio;
    private Double smaLongRatio;
    private Double macd;
    private Double macdSignal;
    private Double bollingerPosition;
    private Double priceChange;
    private Double volumeChange;
    private Double atr;

    // Target label: 1 = price up tomorrow, 0 = price down tomorrow
    private Integer label;
    @Builder.Default
    private Boolean isDeleted = false;
}
