package com.dss.dto.groww;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Response from Groww after placing an order.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrowwOrderResponse {

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("status")
    private String status;             // "PLACED", "REJECTED", "PENDING"

    @JsonProperty("message")
    private String message;

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("tradingSymbol")
    private String tradingSymbol;

    @JsonProperty("transactionType")
    private String transactionType;

    @JsonProperty("orderType")
    private String orderType;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("price")
    private Double price;

    @JsonProperty("averagePrice")
    private Double averagePrice;

    @JsonProperty("filledQuantity")
    private Integer filledQuantity;

    @JsonProperty("pendingQuantity")
    private Integer pendingQuantity;

    @JsonProperty("orderTimestamp")
    private String orderTimestamp;
}
