package com.dss.entity;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TechnicalIndicator {

    private String symbol;
    private LocalDate date;
    private Double rsi;
    private Double smaShort;
    private Double smaLong;
    private Double ema12;
    private Double ema26;
    private Double macd;
    private Double macdSignal;
    private Double macdHistogram;
    private Double bollingerUpper;
    private Double bollingerLower;
    private Double bollingerMiddle;
    private Double atr;
    @Builder.Default
    private Boolean isDeleted = false;
}
