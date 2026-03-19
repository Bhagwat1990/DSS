package com.dss.service;

import com.dss.dto.FeatureEngineeringResponse;
import com.dss.entity.FeatureLabel;
import com.dss.entity.StockData;
import com.dss.entity.TechnicalIndicator;
import com.dss.repository.FeatureLabelRepository;
import com.dss.repository.StockDataRepository;
import com.dss.repository.TechnicalIndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Phase 2/3: Feature Engineering
 * Creates feature vectors with labels from stock data and technical indicators.
 * Label: 1 if price goes up tomorrow, 0 if price goes down.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureEngineeringService {

    private final StockDataRepository stockDataRepository;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final FeatureLabelRepository featureLabelRepository;

    public FeatureEngineeringResponse generateFeatures(String symbol) {
        List<StockData> stockDataList = stockDataRepository.findBySymbolOrderByDateAsc(symbol);
        List<TechnicalIndicator> indicators = technicalIndicatorRepository.findBySymbolOrderByDateAsc(symbol);

        if (stockDataList.size() < 2 || indicators.isEmpty()) {
            return FeatureEngineeringResponse.builder()
                    .symbol(symbol)
                    .featuresGenerated(0)
                    .message("Insufficient data. Fetch stock data and calculate indicators first.")
                    .build();
        }

        // Create a map for quick indicator lookup by date
        Map<java.time.LocalDate, TechnicalIndicator> indicatorMap = new HashMap<>();
        for (TechnicalIndicator ti : indicators) {
            indicatorMap.put(ti.getDate(), ti);
        }

        int generated = 0;
        for (int i = 0; i < stockDataList.size() - 1; i++) {
            StockData today = stockDataList.get(i);
            StockData tomorrow = stockDataList.get(i + 1);
            TechnicalIndicator ti = indicatorMap.get(today.getDate());

            if (ti == null || ti.getRsi() == null || ti.getSmaShort() == null || ti.getSmaLong() == null) {
                continue;
            }

            // Label: 1 if price goes up tomorrow, 0 otherwise
            int label = tomorrow.getClose() > today.getClose() ? 1 : 0;

            // Price change %
            double priceChange = i > 0
                    ? (today.getClose() - stockDataList.get(i - 1).getClose()) / stockDataList.get(i - 1).getClose() * 100
                    : 0;

            // Volume change %
            double volumeChange = i > 0 && stockDataList.get(i - 1).getVolume() != 0
                    ? (double)(today.getVolume() - stockDataList.get(i - 1).getVolume()) / stockDataList.get(i - 1).getVolume() * 100
                    : 0;

            // Bollinger position
            Double bollingerPos = null;
            if (ti.getBollingerUpper() != null && ti.getBollingerLower() != null) {
                double range = ti.getBollingerUpper() - ti.getBollingerLower();
                if (range != 0) {
                    bollingerPos = (today.getClose() - ti.getBollingerLower()) / range;
                }
            }

            FeatureLabel feature = FeatureLabel.builder()
                    .symbol(symbol)
                    .date(today.getDate())
                    .rsi(ti.getRsi())
                    .smaShortRatio(today.getClose() / ti.getSmaShort())
                    .smaLongRatio(today.getClose() / ti.getSmaLong())
                    .macd(ti.getMacd())
                    .macdSignal(ti.getMacdSignal())
                    .bollingerPosition(bollingerPos)
                    .priceChange(priceChange)
                    .volumeChange(volumeChange)
                    .atr(ti.getAtr())
                    .label(label)
                    .build();

            featureLabelRepository.save(feature);
            generated++;
        }

        log.info("Generated {} feature-label records for {}", generated, symbol);
        return FeatureEngineeringResponse.builder()
                .symbol(symbol)
                .featuresGenerated(generated)
                .message("Successfully generated " + generated + " feature-label records")
                .build();
    }
}
