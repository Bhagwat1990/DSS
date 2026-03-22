package com.dss.entity;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Stores a snapshot of a Groww live quote for historical tracking.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuoteHistory {

    private String symbol;
    private LocalDateTime timestamp;
    private Double lastPrice;
    private Double dayChange;
    private Double dayChangePerc;
    private Long volume;
    private Double week52High;
    private Double week52Low;
    private Double upperCircuit;
    private Double lowerCircuit;
    @Builder.Default
    private Boolean isDeleted = false;
}
