package com.dss.dto;

import lombok.*;

/**
 * Request to fetch and store stock data for a given symbol.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockDataRequest {
    private String symbol;        // e.g., "RELIANCE.BSE", "TCS.BSE", "NIFTY50"
    private String outputSize;    // "compact" (100 days) or "full" (20+ years)
}
