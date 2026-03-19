package com.dss.repository;

import com.dss.entity.StockData;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockDataRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<StockData> ROW_MAPPER = (rs, rowNum) -> StockData.builder()
            .symbol(rs.getString("symbol"))
            .date(rs.getTimestamp("ts").toLocalDateTime().toLocalDate())
            .open(getDoubleOrNull(rs, "open"))
            .high(getDoubleOrNull(rs, "high"))
            .low(getDoubleOrNull(rs, "low"))
            .close(getDoubleOrNull(rs, "close"))
            .volume(getLongOrNull(rs, "volume"))
            .build();

    public void save(StockData sd) {
        jdbcTemplate.update(
                "INSERT INTO stock_data(symbol, ts, open, high, low, close, volume) VALUES (?, ?, ?, ?, ?, ?, ?)",
                sd.getSymbol(),
                Timestamp.valueOf(sd.getDate().atStartOfDay()),
                sd.getOpen(), sd.getHigh(), sd.getLow(), sd.getClose(), sd.getVolume());
    }

    public List<StockData> findBySymbolOrderByDateAsc(String symbol) {
        return jdbcTemplate.query(
                "SELECT * FROM stock_data WHERE symbol = ? ORDER BY ts ASC",
                ROW_MAPPER, symbol);
    }

    public Optional<StockData> findBySymbolAndDate(String symbol, LocalDate date) {
        List<StockData> results = jdbcTemplate.query(
                "SELECT * FROM stock_data WHERE symbol = ? AND ts = ? LIMIT 1",
                ROW_MAPPER, symbol, Timestamp.valueOf(date.atStartOfDay()));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<StockData> findTopBySymbolOrderByDateDesc(String symbol) {
        List<StockData> results = jdbcTemplate.query(
                "SELECT * FROM stock_data LATEST ON ts PARTITION BY symbol WHERE symbol = ?",
                ROW_MAPPER, symbol);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private static Double getDoubleOrNull(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    private static Long getLongOrNull(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }
}
