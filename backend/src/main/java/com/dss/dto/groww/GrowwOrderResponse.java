package com.dss.dto.groww;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

/**
 * Response from Groww Trading API after placing an order.
 * <p>
 * Endpoint: POST https://api.groww.in/v1/order/create
 * Docs: https://groww.in/trade-api/docs/curl/orders
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrowwOrderResponse {

    @JsonProperty("status")
    private String status;               // "SUCCESS" or "FAILURE"

    @JsonProperty("payload")
    private OrderPayload payload;

    @JsonProperty("error")
    private Map<String, Object> error;   // present when status=FAILURE

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderPayload {

        @JsonProperty("groww_order_id")
        private String growwOrderId;

        @JsonProperty("order_status")
        private String orderStatus;      // "OPEN", "REJECTED", etc.

        @JsonProperty("order_reference_id")
        private String orderReferenceId;

        @JsonProperty("remark")
        private String remark;
    }
}
