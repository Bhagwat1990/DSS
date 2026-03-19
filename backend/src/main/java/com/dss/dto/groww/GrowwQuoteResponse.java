package com.dss.dto.groww;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Quote response from Groww's live pricing endpoint.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrowwQuoteResponse {

    @JsonProperty("isin")
    private String isin;

    @JsonProperty("bseScriptCode")
    private String bseScriptCode;

    @JsonProperty("nseScriptCode")
    private String nseScriptCode;

    @JsonProperty("growwContractId")
    private String growwContractId;

    @JsonProperty("companyName")
    private String companyName;

    @JsonProperty("searchId")
    private String searchId;

    @JsonProperty("ltp")
    private Double ltp;               // Last Traded Price

    @JsonProperty("dayChange")
    private Double dayChange;

    @JsonProperty("dayChangePerc")
    private Double dayChangePerc;

    @JsonProperty("high")
    private Double high;

    @JsonProperty("low")
    private Double low;

    @JsonProperty("open")
    private Double open;

    @JsonProperty("close")
    private Double close;             // Previous close

    @JsonProperty("volume")
    private Long volume;

    @JsonProperty("tsInMillis")
    private Long tsInMillis;

    @JsonProperty("livePriceDto")
    private LivePriceDto livePriceDto;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LivePriceDto {
        @JsonProperty("ltp")
        private Double ltp;

        @JsonProperty("dayChange")
        private Double dayChange;

        @JsonProperty("dayChangePerc")
        private Double dayChangePerc;

        @JsonProperty("high")
        private Double high;

        @JsonProperty("low")
        private Double low;

        @JsonProperty("open")
        private Double open;

        @JsonProperty("close")
        private Double close;

        @JsonProperty("volume")
        private Long volume;

        @JsonProperty("tsInMillis")
        private Long tsInMillis;

        @JsonProperty("lowerCircuit")
        private Double lowerCircuit;

        @JsonProperty("upperCircuit")
        private Double upperCircuit;

        @JsonProperty("totalBuyQty")
        private Long totalBuyQty;

        @JsonProperty("totalSellQty")
        private Long totalSellQty;
    }
}
