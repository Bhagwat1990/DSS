package com.dss.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockDataResponse {
    private String symbol;
    private int recordCount;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String message;
    private List<StockDataPoint> data;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StockDataPoint {
        private LocalDate date;
        private Double open;
        private Double high;
        private Double low;
        private Double close;
        private Long volume;
    }
}
