package com.dss.dto.groww;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Request body for placing an order via Groww Trading API.
 * <p>
 * Endpoint: POST https://api.groww.in/v1/order/create
 * Docs: https://groww.in/trade-api/docs/curl/orders
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrowwOrderRequest {

    @JsonProperty("trading_symbol")
    private String tradingSymbol;      // e.g. "RELIANCE", "WIPRO"

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("price")
    private Double price;              // Required for LIMIT / SL orders

    @JsonProperty("trigger_price")
    private Double triggerPrice;       // Required for SL / SL-M orders

    @JsonProperty("validity")
    private String validity;           // "DAY", "IOC"

    @JsonProperty("exchange")
    private String exchange;           // "NSE" or "BSE"

    @JsonProperty("segment")
    private String segment;            // "CASH", "FNO"

    @JsonProperty("product")
    private String product;            // "CNC", "INTRADAY", "MARGIN"

    @JsonProperty("order_type")
    private String orderType;          // "MARKET", "LIMIT", "SL", "SL-M"

    @JsonProperty("transaction_type")
    private String transactionType;    // "BUY" or "SELL"

    @JsonProperty("order_reference_id")
    private String orderReferenceId;   // User-provided 8-20 char alphanumeric ID
}
