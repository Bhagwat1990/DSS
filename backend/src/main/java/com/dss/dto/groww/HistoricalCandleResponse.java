package com.dss.dto.groww;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Response from Groww Historical Candle Data API.
 * <p>
 * Endpoint: GET https://api.groww.in/v1/historical/candles
 * Docs: https://groww.in/trade-api/docs/curl/backtesting#get-historical-candle-data
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricalCandleResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("payload")
    private CandlePayload payload;

    @JsonProperty("error")
    private Map<String, Object> error;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CandlePayload {

        /** Each candle: [timestamp, open, high, low, close, volume, openInterest] */
        @JsonProperty("candles")
        private List<List<Object>> candles;

        @JsonProperty("closing_price")
        private Double closingPrice;

        @JsonProperty("start_time")
        private String startTime;

        @JsonProperty("end_time")
        private String endTime;

        @JsonProperty("interval_in_minutes")
        private Integer intervalInMinutes;
    }
}
