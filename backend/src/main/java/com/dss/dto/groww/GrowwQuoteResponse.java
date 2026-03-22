package com.dss.dto.groww;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Quote response from Groww's official Trading API.
 * <p>
 * Endpoint: GET https://api.groww.in/v1/live-data/quote
 * Docs: https://groww.in/trade-api/docs/curl/live-data
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrowwQuoteResponse {

    @JsonProperty("status")
    private String status;                 // "SUCCESS" or "FAILURE"

    @JsonProperty("payload")
    private QuotePayload payload;

    @JsonProperty("error")
    private Map<String, Object> error;     // present when status=FAILURE

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuotePayload {

        @JsonProperty("average_price")
        private Double averagePrice;

        @JsonProperty("bid_quantity")
        private Integer bidQuantity;

        @JsonProperty("bid_price")
        private Double bidPrice;

        @JsonProperty("day_change")
        private Double dayChange;

        @JsonProperty("day_change_perc")
        private Double dayChangePerc;

        @JsonProperty("upper_circuit_limit")
        private Double upperCircuitLimit;

        @JsonProperty("lower_circuit_limit")
        private Double lowerCircuitLimit;

        @JsonProperty("open")
        private Double open;

        @JsonProperty("high")
        private Double high;

        @JsonProperty("low")
        private Double low;

        @JsonProperty("close")
        private Double close;

        @JsonProperty("last_price")
        private Double lastPrice;

        @JsonProperty("last_trade_quantity")
        private Integer lastTradeQuantity;

        @JsonProperty("last_trade_time")
        private Long lastTradeTime;

        @JsonProperty("volume")
        private Integer volume;

        @JsonProperty("total_buy_quantity")
        private Double totalBuyQuantity;

        @JsonProperty("total_sell_quantity")
        private Double totalSellQuantity;

        @JsonProperty("offer_price")
        private Double offerPrice;

        @JsonProperty("offer_quantity")
        private Integer offerQuantity;

        @JsonProperty("market_cap")
        private Double marketCap;

        @JsonProperty("week_52_high")
        private Double week52High;

        @JsonProperty("week_52_low")
        private Double week52Low;

        @JsonProperty("high_trade_range")
        private Double highTradeRange;

        @JsonProperty("low_trade_range")
        private Double lowTradeRange;

        @JsonProperty("implied_volatility")
        private Double impliedVolatility;

        @JsonProperty("open_interest")
        private Double openInterest;

        @JsonProperty("previous_open_interest")
        private Double previousOpenInterest;

        @JsonProperty("oi_day_change")
        private Double oiDayChange;

        @JsonProperty("oi_day_change_percentage")
        private Double oiDayChangePercentage;

        @JsonProperty("depth")
        private MarketDepth depth;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MarketDepth {
        @JsonProperty("buy")
        private List<DepthEntry> buy;

        @JsonProperty("sell")
        private List<DepthEntry> sell;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DepthEntry {
        @JsonProperty("price")
        private Double price;

        @JsonProperty("quantity")
        private Integer quantity;
    }
}
