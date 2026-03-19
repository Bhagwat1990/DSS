package com.dss.service;

import com.dss.entity.StockData;
import com.dss.entity.TechnicalIndicator;
import com.dss.repository.StockDataRepository;
import com.dss.repository.TechnicalIndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2: Feature Engineering - Technical Indicators
 * Calculates RSI, SMA, EMA, MACD, Bollinger Bands, ATR from raw stock data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicalIndicatorService {

    private final StockDataRepository stockDataRepository;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;

    /**
     * Calculate and store all technical indicators for a given symbol.
     */
    public int calculateAndStoreIndicators(String symbol) {
        List<StockData> stockDataList = stockDataRepository.findBySymbolOrderByDateAsc(symbol);

        if (stockDataList.size() < 50) {
            log.warn("Not enough data to calculate indicators for {}. Need at least 50 data points, have {}",
                    symbol, stockDataList.size());
            return 0;
        }

        double[] closes = stockDataList.stream().mapToDouble(StockData::getClose).toArray();
        double[] highs = stockDataList.stream().mapToDouble(StockData::getHigh).toArray();
        double[] lows = stockDataList.stream().mapToDouble(StockData::getLow).toArray();

        double[] rsi = calculateRSI(closes, 14);
        double[] sma20 = calculateSMA(closes, 20);
        double[] sma50 = calculateSMA(closes, 50);
        double[] ema12 = calculateEMA(closes, 12);
        double[] ema26 = calculateEMA(closes, 26);
        double[] macd = new double[closes.length];
        for (int i = 0; i < closes.length; i++) {
            macd[i] = ema12[i] - ema26[i];
        }
        double[] macdSignal = calculateEMA(macd, 9);
        double[] macdHistogram = new double[closes.length];
        for (int i = 0; i < closes.length; i++) {
            macdHistogram[i] = macd[i] - macdSignal[i];
        }

        double[][] bollinger = calculateBollingerBands(closes, 20);
        double[] atr = calculateATR(highs, lows, closes, 14);

        List<TechnicalIndicator> indicators = new ArrayList<>();
        // Start from index 49 (need at least 50 data points for SMA50)
        for (int i = 49; i < stockDataList.size(); i++) {
            StockData sd = stockDataList.get(i);

            TechnicalIndicator indicator = TechnicalIndicator.builder()
                    .symbol(symbol)
                    .date(sd.getDate())
                    .rsi(validOrNull(rsi[i]))
                    .smaShort(validOrNull(sma20[i]))
                    .smaLong(validOrNull(sma50[i]))
                    .ema12(validOrNull(ema12[i]))
                    .ema26(validOrNull(ema26[i]))
                    .macd(validOrNull(macd[i]))
                    .macdSignal(validOrNull(macdSignal[i]))
                    .macdHistogram(validOrNull(macdHistogram[i]))
                    .bollingerUpper(validOrNull(bollinger[0][i]))
                    .bollingerMiddle(validOrNull(bollinger[1][i]))
                    .bollingerLower(validOrNull(bollinger[2][i]))
                    .atr(validOrNull(atr[i]))
                    .build();

            // QuestDB DEDUP UPSERT KEYS handles duplicates automatically
            technicalIndicatorRepository.save(indicator);
            indicators.add(indicator);
        }

        log.info("Calculated and stored {} technical indicators for {}", indicators.size(), symbol);
        return indicators.size();
    }

    // ==================== RSI ====================
    private double[] calculateRSI(double[] closes, int period) {
        double[] rsi = new double[closes.length];
        double[] gains = new double[closes.length];
        double[] losses = new double[closes.length];

        for (int i = 1; i < closes.length; i++) {
            double change = closes[i] - closes[i - 1];
            gains[i] = change > 0 ? change : 0;
            losses[i] = change < 0 ? -change : 0;
        }

        double avgGain = 0, avgLoss = 0;
        for (int i = 1; i <= period; i++) {
            avgGain += gains[i];
            avgLoss += losses[i];
        }
        avgGain /= period;
        avgLoss /= period;

        rsi[period] = avgLoss == 0 ? 100 : 100 - (100 / (1 + avgGain / avgLoss));

        for (int i = period + 1; i < closes.length; i++) {
            avgGain = (avgGain * (period - 1) + gains[i]) / period;
            avgLoss = (avgLoss * (period - 1) + losses[i]) / period;
            rsi[i] = avgLoss == 0 ? 100 : 100 - (100 / (1 + avgGain / avgLoss));
        }

        return rsi;
    }

    // ==================== SMA ====================
    private double[] calculateSMA(double[] data, int period) {
        double[] sma = new double[data.length];
        for (int i = period - 1; i < data.length; i++) {
            double sum = 0;
            for (int j = i - period + 1; j <= i; j++) {
                sum += data[j];
            }
            sma[i] = sum / period;
        }
        return sma;
    }

    // ==================== EMA ====================
    private double[] calculateEMA(double[] data, int period) {
        double[] ema = new double[data.length];
        double multiplier = 2.0 / (period + 1);

        // Start EMA with SMA for the first value
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += data[i];
        }
        ema[period - 1] = sum / period;

        for (int i = period; i < data.length; i++) {
            ema[i] = (data[i] - ema[i - 1]) * multiplier + ema[i - 1];
        }

        return ema;
    }

    // ==================== Bollinger Bands ====================
    private double[][] calculateBollingerBands(double[] closes, int period) {
        double[] upper = new double[closes.length];
        double[] middle = new double[closes.length];
        double[] lower = new double[closes.length];

        double[] sma = calculateSMA(closes, period);

        for (int i = period - 1; i < closes.length; i++) {
            double sumSqDiff = 0;
            for (int j = i - period + 1; j <= i; j++) {
                sumSqDiff += Math.pow(closes[j] - sma[i], 2);
            }
            double stdDev = Math.sqrt(sumSqDiff / period);
            middle[i] = sma[i];
            upper[i] = sma[i] + 2 * stdDev;
            lower[i] = sma[i] - 2 * stdDev;
        }

        return new double[][] { upper, middle, lower };
    }

    // ==================== ATR ====================
    private double[] calculateATR(double[] highs, double[] lows, double[] closes, int period) {
        double[] tr = new double[closes.length];
        double[] atr = new double[closes.length];

        tr[0] = highs[0] - lows[0];
        for (int i = 1; i < closes.length; i++) {
            double hl = highs[i] - lows[i];
            double hc = Math.abs(highs[i] - closes[i - 1]);
            double lc = Math.abs(lows[i] - closes[i - 1]);
            tr[i] = Math.max(hl, Math.max(hc, lc));
        }

        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += tr[i];
        }
        atr[period - 1] = sum / period;

        for (int i = period; i < closes.length; i++) {
            atr[i] = (atr[i - 1] * (period - 1) + tr[i]) / period;
        }

        return atr;
    }

    private Double validOrNull(double value) {
        return Double.isNaN(value) || value == 0.0 ? null : value;
    }
}
