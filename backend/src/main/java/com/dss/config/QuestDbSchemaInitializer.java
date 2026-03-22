package com.dss.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates QuestDB tables on application startup.
 * Uses QuestDB-specific SQL: SYMBOL type, designated timestamp, WAL mode, DEDUP UPSERT KEYS.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuestDbSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Initializing QuestDB schema...");
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            log.warn("QuestDB is not reachable – skipping schema initialization: {}", e.getMessage());
            return;
        }

        createTable("stock_data",
                "CREATE TABLE IF NOT EXISTS stock_data (" +
                "  symbol SYMBOL, " +
                "  ts TIMESTAMP, " +
                "  open DOUBLE, " +
                "  high DOUBLE, " +
                "  low DOUBLE, " +
                "  close DOUBLE, " +
                "  volume LONG, " +
                "  is_deleted BOOLEAN" +
                ") timestamp(ts) PARTITION BY MONTH WAL " +
                "DEDUP UPSERT KEYS(symbol, ts)");

        createTable("technical_indicators",
                "CREATE TABLE IF NOT EXISTS technical_indicators (" +
                "  symbol SYMBOL, " +
                "  ts TIMESTAMP, " +
                "  rsi DOUBLE, " +
                "  sma_short DOUBLE, " +
                "  sma_long DOUBLE, " +
                "  ema12 DOUBLE, " +
                "  ema26 DOUBLE, " +
                "  macd DOUBLE, " +
                "  macd_signal DOUBLE, " +
                "  macd_histogram DOUBLE, " +
                "  bollinger_upper DOUBLE, " +
                "  bollinger_lower DOUBLE, " +
                "  bollinger_middle DOUBLE, " +
                "  atr DOUBLE, " +
                "  is_deleted BOOLEAN" +
                ") timestamp(ts) PARTITION BY MONTH WAL " +
                "DEDUP UPSERT KEYS(symbol, ts)");

        createTable("feature_labels",
                "CREATE TABLE IF NOT EXISTS feature_labels (" +
                "  symbol SYMBOL, " +
                "  ts TIMESTAMP, " +
                "  rsi DOUBLE, " +
                "  sma_short_ratio DOUBLE, " +
                "  sma_long_ratio DOUBLE, " +
                "  macd DOUBLE, " +
                "  macd_signal DOUBLE, " +
                "  bollinger_position DOUBLE, " +
                "  price_change DOUBLE, " +
                "  volume_change DOUBLE, " +
                "  atr DOUBLE, " +
                "  label INT, " +
                "  is_deleted BOOLEAN" +
                ") timestamp(ts) PARTITION BY MONTH WAL " +
                "DEDUP UPSERT KEYS(symbol, ts)");

        createTable("prediction_results",
                "CREATE TABLE IF NOT EXISTS prediction_results (" +
                "  symbol SYMBOL, " +
                "  prediction_time TIMESTAMP, " +
                "  ml_prediction INT, " +
                "  ml_confidence DOUBLE, " +
                "  signal STRING, " +
                "  ai_suggestion STRING, " +
                "  input_summary STRING, " +
                "  is_deleted BOOLEAN" +
                ") timestamp(prediction_time) PARTITION BY MONTH WAL");

        createTable("quote_history",
                "CREATE TABLE IF NOT EXISTS quote_history (" +
                "  symbol SYMBOL, " +
                "  ts TIMESTAMP, " +
                "  last_price DOUBLE, " +
                "  day_change DOUBLE, " +
                "  day_change_perc DOUBLE, " +
                "  volume LONG, " +
                "  week_52_high DOUBLE, " +
                "  week_52_low DOUBLE, " +
                "  upper_circuit DOUBLE, " +
                "  lower_circuit DOUBLE, " +
                "  is_deleted BOOLEAN" +
                ") timestamp(ts) PARTITION BY MONTH WAL " +
                "DEDUP UPSERT KEYS(symbol, ts)");

        addColumnIfNotExists("stock_data", "is_deleted", "BOOLEAN");
        addColumnIfNotExists("technical_indicators", "is_deleted", "BOOLEAN");
        addColumnIfNotExists("feature_labels", "is_deleted", "BOOLEAN");
        addColumnIfNotExists("prediction_results", "is_deleted", "BOOLEAN");
        addColumnIfNotExists("quote_history", "is_deleted", "BOOLEAN");

        log.info("QuestDB schema initialization complete.");
    }

    private void createTable(String name, String ddl) {
        try {
            jdbcTemplate.execute(ddl);
            log.info("Table '{}' ready.", name);
        } catch (Exception e) {
            log.warn("Could not create table '{}': {}", name, e.getMessage());
        }
    }

    private void addColumnIfNotExists(String table, String column, String type) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            log.info("Added column '{}' to table '{}'.", column, table);
        } catch (Exception e) {
            // Column already exists — safe to ignore
            log.debug("Column '{}' already exists on '{}': {}", column, table, e.getMessage());
        }
    }
}
