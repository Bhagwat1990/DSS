package com.dss.dto.groww;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Request body for placing an order via Groww.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrowwOrderRequest {

    @JsonProperty("exchange")
    private String exchange;           // "NSE" or "BSE"

    @JsonProperty("tradingSymbol")
    private String tradingSymbol;      // e.g. "RELIANCE", "TCS"

    @JsonProperty("transactionType")
    private String transactionType;    // "BUY" or "SELL"

    @JsonProperty("orderType")
    private String orderType;          // "MARKET", "LIMIT", "SL", "SL-M"

    @JsonProperty("productType")
    private String productType;        // "CNC" (delivery), "INTRADAY", "MARGIN"

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("price")
    private Double price;              // Required for LIMIT / SL orders

    @JsonProperty("triggerPrice")
    private Double triggerPrice;       // Required for SL / SL-M orders

    @JsonProperty("disclosedQuantity")
    private Integer disclosedQuantity; // Optional
}
