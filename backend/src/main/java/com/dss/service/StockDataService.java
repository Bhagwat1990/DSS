package com.dss.service;

import com.dss.dto.StockDataRequest;
import com.dss.dto.StockDataResponse;
import com.dss.entity.StockData;
import com.dss.repository.StockDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 1: Data Pipeline
 * Fetches stock data from Alpha Vantage API and stores it in the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockDataService {

    private final WebClient webClient;
    private final StockDataRepository stockDataRepository;
    private final ObjectMapper objectMapper;

    @Value("${alpha-vantage.api.key}")
    private String apiKey;

    @Value("${alpha-vantage.api.url}")
    private String apiUrl;

    /**
     * Fetch daily stock data from Alpha Vantage and persist to database.
     */
    public StockDataResponse fetchAndStoreStockData(StockDataRequest request) {
        String symbol = request.getSymbol();
        String outputSize = request.getOutputSize() != null ? request.getOutputSize() : "compact";

        log.info("Fetching stock data for symbol: {}, outputSize: {}", symbol, outputSize);

        try {
            String response = webClient.get()
                    .uri(apiUrl + "?function=TIME_SERIES_DAILY&symbol={symbol}&outputsize={outputSize}&apikey={apiKey}",
                            symbol, outputSize, apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode timeSeries = root.get("Time Series (Daily)");

            if (timeSeries == null) {
                log.warn("No time series data found for symbol: {}. Response: {}", symbol, response);
                return StockDataResponse.builder()
                        .symbol(symbol)
                        .recordCount(0)
                        .message("No data found. Check symbol or API key. Response: " +
                                (root.has("Note") ? root.get("Note").asText() : root.toString()))
                        .build();
            }

            List<StockData> stockDataList = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            Iterator<Map.Entry<String, JsonNode>> fields = timeSeries.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                LocalDate date = LocalDate.parse(entry.getKey(), formatter);
                JsonNode values = entry.getValue();

                StockData stockData = StockData.builder()
                        .symbol(symbol)
                        .date(date)
                        .open(values.get("1. open").asDouble())
                        .high(values.get("2. high").asDouble())
                        .low(values.get("3. low").asDouble())
                        .close(values.get("4. close").asDouble())
                        .volume(values.get("5. volume").asLong())
                        .build();

                // QuestDB DEDUP UPSERT KEYS handles duplicates automatically
                stockDataRepository.save(stockData);
                stockDataList.add(stockData);
            }

            stockDataList.sort(Comparator.comparing(StockData::getDate));

            List<StockDataResponse.StockDataPoint> dataPoints = stockDataList.stream()
                    .map(sd -> StockDataResponse.StockDataPoint.builder()
                            .date(sd.getDate())
                            .open(sd.getOpen())
                            .high(sd.getHigh())
                            .low(sd.getLow())
                            .close(sd.getClose())
                            .volume(sd.getVolume())
                            .build())
                    .collect(Collectors.toList());

            return StockDataResponse.builder()
                    .symbol(symbol)
                    .recordCount(dataPoints.size())
                    .fromDate(dataPoints.isEmpty() ? null : dataPoints.get(0).getDate())
                    .toDate(dataPoints.isEmpty() ? null : dataPoints.get(dataPoints.size() - 1).getDate())
                    .message("Successfully fetched and stored " + dataPoints.size() + " records")
                    .data(dataPoints)
                    .build();

        } catch (Exception e) {
            log.error("Error fetching stock data for symbol: {}", symbol, e);
            return StockDataResponse.builder()
                    .symbol(symbol)
                    .recordCount(0)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Retrieve stored stock data from database.
     */
    public StockDataResponse getStoredStockData(String symbol) {
        List<StockData> stockDataList = stockDataRepository.findBySymbolOrderByDateAsc(symbol);
        List<StockDataResponse.StockDataPoint> dataPoints = stockDataList.stream()
                .map(sd -> StockDataResponse.StockDataPoint.builder()
                        .date(sd.getDate())
                        .open(sd.getOpen())
                        .high(sd.getHigh())
                        .low(sd.getLow())
                        .close(sd.getClose())
                        .volume(sd.getVolume())
                        .build())
                .collect(Collectors.toList());

        return StockDataResponse.builder()
                .symbol(symbol)
                .recordCount(dataPoints.size())
                .fromDate(dataPoints.isEmpty() ? null : dataPoints.get(0).getDate())
                .toDate(dataPoints.isEmpty() ? null : dataPoints.get(dataPoints.size() - 1).getDate())
                .message(dataPoints.isEmpty() ? "No stored data found" : "Retrieved " + dataPoints.size() + " records")
                .data(dataPoints)
                .build();
    }
}
