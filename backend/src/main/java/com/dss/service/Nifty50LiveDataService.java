package com.dss.service;

import com.dss.client.GrowwApiClient;
import com.dss.dto.groww.GrowwQuoteResponse;
import com.dss.entity.StockData;
import com.dss.repository.StockDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fetches live quote data for all Nifty 50 constituent stocks via the Groww API
 * at a configurable interval and stores each tick into the stock_data table
 * (with millisecond-precision timestamps) for downstream AI analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Nifty50LiveDataService {

    private final GrowwApiClient growwApiClient;
    private final StockDataRepository stockDataRepository;

    /** Current Nifty 50 index constituents (NSE trading symbols). */
    private static final List<String> NIFTY_50_SYMBOLS = List.of(
            "ADANIENT", "ADANIPORTS", "APOLLOHOSP", "ASIANPAINT", "AXISBANK",
            "BAJAJ-AUTO", "BAJFINANCE", "BAJAJFINSV", "BEL", "BPCL",
            "BHARTIARTL", "BRITANNIA", "CIPLA", "COALINDIA", "DRREDDY",
            "EICHERMOT", "ETERNAL", "GRASIM", "HCLTECH", "HDFCBANK",
            "HDFCLIFE", "HEROMOTOCO", "HINDALCO", "HINDUNILVR", "ICICIBANK",
            "ITC", "INDUSINDBK", "INFY", "JSWSTEEL", "KOTAKBANK",
            "LT", "M&M", "MARUTI", "NESTLEIND", "NTPC",
            "ONGC", "POWERGRID", "RELIANCE", "SBILIFE", "SBIN",
            "SUNPHARMA", "TCS", "TATACONSUM", "TATAMOTORS", "TATASTEEL",
            "TECHM", "TITAN", "TRENT", "ULTRACEMCO", "WIPRO"
    );

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong tickCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private volatile long intervalMs = 1000; // default 1 second

    /**
     * Start the live data feed at the given interval.
     *
     * @param intervalMillis polling interval in milliseconds (minimum 100ms)
     */
    public synchronized void start(long intervalMillis) {
        if (running.get()) {
            log.warn("Nifty 50 live feed is already running");
            return;
        }
        this.intervalMs = Math.max(intervalMillis, 100); // floor at 100ms
        this.tickCount.set(0);
        this.errorCount.set(0);
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = Thread.ofVirtual().unstarted(r);
            t.setName("nifty50-live-feed");
            return t;
        });
        this.scheduledTask = scheduler.scheduleAtFixedRate(
                this::fetchAllAndStore, 0, this.intervalMs, TimeUnit.MILLISECONDS);
        running.set(true);
        log.info("Nifty 50 live feed STARTED — interval={}ms, symbols={}", this.intervalMs, NIFTY_50_SYMBOLS.size());
    }

    /**
     * Stop the live data feed.
     */
    public synchronized void stop() {
        if (!running.get()) {
            log.warn("Nifty 50 live feed is not running");
            return;
        }
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        running.set(false);
        log.info("Nifty 50 live feed STOPPED — totalTicks={}, errors={}", tickCount.get(), errorCount.get());
    }

    /**
     * Returns the current status of the live feed.
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("running", running.get());
        status.put("intervalMs", intervalMs);
        status.put("totalTicks", tickCount.get());
        status.put("errors", errorCount.get());
        status.put("symbolCount", NIFTY_50_SYMBOLS.size());
        status.put("symbols", NIFTY_50_SYMBOLS);
        return status;
    }

    /**
     * Fetch a single tick for all Nifty 50 stocks and return stored records.
     * Can be called independently via the REST API for a one-shot fetch.
     */
    public List<StockData> fetchOnce() {
        return fetchAllAndStoreReturning();
    }

    public List<String> getSymbols() {
        return Collections.unmodifiableList(NIFTY_50_SYMBOLS);
    }

    // ─────────────────────── internals ───────────────────────

    private void fetchAllAndStore() {
        try {
            fetchAllAndStoreReturning();
            tickCount.incrementAndGet();
        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("Nifty 50 live feed tick failed: {}", e.getMessage());
        }
    }

    private List<StockData> fetchAllAndStoreReturning() {
        LocalDateTime now = LocalDateTime.now();
        List<StockData> results = new ArrayList<>();

        for (String symbol : NIFTY_50_SYMBOLS) {
            try {
                GrowwQuoteResponse resp = growwApiClient.getQuote("NSE", symbol);
                GrowwQuoteResponse.QuotePayload p = resp.getPayload();
                if (p == null) {
                    log.warn("No payload for {}", symbol);
                    continue;
                }

                StockData sd = StockData.builder()
                        .symbol(symbol)
                        .date(now.toLocalDate())
                        .open(p.getOpen())
                        .high(p.getHigh())
                        .low(p.getLow())
                        .close(p.getLastPrice())   // live: last traded price as close
                        .volume(p.getVolume() != null ? p.getVolume().longValue() : null)
                        .build();

                stockDataRepository.saveLive(sd, now);
                results.add(sd);
            } catch (Exception e) {
                log.warn("Failed to fetch quote for {}: {}", symbol, e.getMessage());
            }
        }

        log.debug("Nifty 50 tick stored {} / {} symbols at {}", results.size(), NIFTY_50_SYMBOLS.size(), now);
        return results;
    }
}
