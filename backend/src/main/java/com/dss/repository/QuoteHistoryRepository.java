package com.dss.repository;

import com.dss.entity.QuoteHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class QuoteHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<QuoteHistory> ROW_MAPPER = (rs, rowNum) -> QuoteHistory.builder()
            .symbol(rs.getString("symbol"))
            .timestamp(rs.getTimestamp("ts").toLocalDateTime())
            .lastPrice(getDoubleOrNull(rs, "last_price"))
            .dayChange(getDoubleOrNull(rs, "day_change"))
            .dayChangePerc(getDoubleOrNull(rs, "day_change_perc"))
            .volume(getLongOrNull(rs, "volume"))
            .week52High(getDoubleOrNull(rs, "week_52_high"))
            .week52Low(getDoubleOrNull(rs, "week_52_low"))
            .upperCircuit(getDoubleOrNull(rs, "upper_circuit"))
            .lowerCircuit(getDoubleOrNull(rs, "lower_circuit"))
            .isDeleted(getBooleanOrNull(rs, "is_deleted"))
            .build();

    public void save(QuoteHistory qh) {
        jdbcTemplate.update(
                "INSERT INTO quote_history(symbol, ts, last_price, day_change, day_change_perc, " +
                "volume, week_52_high, week_52_low, upper_circuit, lower_circuit, is_deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                qh.getSymbol(), Timestamp.valueOf(qh.getTimestamp()),
                qh.getLastPrice(), qh.getDayChange(), qh.getDayChangePerc(),
                qh.getVolume(), qh.getWeek52High(), qh.getWeek52Low(),
                qh.getUpperCircuit(), qh.getLowerCircuit(),
                qh.getIsDeleted() != null && qh.getIsDeleted());
    }

    public List<QuoteHistory> findBySymbolOrderByTimestampDesc(String symbol) {
        return jdbcTemplate.query(
                "SELECT * FROM quote_history WHERE symbol = ? AND (is_deleted != true OR is_deleted IS NULL) ORDER BY ts DESC",
                ROW_MAPPER, symbol);
    }

    public List<QuoteHistory> findAllLatest() {
        return jdbcTemplate.query(
                "SELECT * FROM quote_history WHERE is_deleted != true OR is_deleted IS NULL LATEST ON ts PARTITION BY symbol ORDER BY symbol",
                ROW_MAPPER);
    }

    public List<QuoteHistory> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM quote_history WHERE is_deleted != true OR is_deleted IS NULL ORDER BY ts DESC",
                ROW_MAPPER);
    }

    public void deleteBySymbol(String symbol) {
        jdbcTemplate.update(
                "UPDATE quote_history SET is_deleted = true WHERE symbol = ?", symbol);
    }

    private static Double getDoubleOrNull(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    private static Long getLongOrNull(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    private static Boolean getBooleanOrNull(ResultSet rs, String col) throws SQLException {
        boolean v = rs.getBoolean(col);
        return rs.wasNull() ? null : v;
    }
}
