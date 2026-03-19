package com.dss.repository;

import com.dss.entity.TechnicalIndicator;
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
public class TechnicalIndicatorRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<TechnicalIndicator> ROW_MAPPER = (rs, rowNum) -> TechnicalIndicator.builder()
            .symbol(rs.getString("symbol"))
            .date(rs.getTimestamp("ts").toLocalDateTime().toLocalDate())
            .rsi(getDoubleOrNull(rs, "rsi"))
            .smaShort(getDoubleOrNull(rs, "sma_short"))
            .smaLong(getDoubleOrNull(rs, "sma_long"))
            .ema12(getDoubleOrNull(rs, "ema12"))
            .ema26(getDoubleOrNull(rs, "ema26"))
            .macd(getDoubleOrNull(rs, "macd"))
            .macdSignal(getDoubleOrNull(rs, "macd_signal"))
            .macdHistogram(getDoubleOrNull(rs, "macd_histogram"))
            .bollingerUpper(getDoubleOrNull(rs, "bollinger_upper"))
            .bollingerLower(getDoubleOrNull(rs, "bollinger_lower"))
            .bollingerMiddle(getDoubleOrNull(rs, "bollinger_middle"))
            .atr(getDoubleOrNull(rs, "atr"))
            .build();

    public void save(TechnicalIndicator ti) {
        jdbcTemplate.update(
                "INSERT INTO technical_indicators(symbol, ts, rsi, sma_short, sma_long, ema12, ema26, " +
                "macd, macd_signal, macd_histogram, bollinger_upper, bollinger_lower, bollinger_middle, atr) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ti.getSymbol(), Timestamp.valueOf(ti.getDate().atStartOfDay()),
                ti.getRsi(), ti.getSmaShort(), ti.getSmaLong(), ti.getEma12(), ti.getEma26(),
                ti.getMacd(), ti.getMacdSignal(), ti.getMacdHistogram(),
                ti.getBollingerUpper(), ti.getBollingerLower(), ti.getBollingerMiddle(), ti.getAtr());
    }

    public List<TechnicalIndicator> findBySymbolOrderByDateAsc(String symbol) {
        return jdbcTemplate.query(
                "SELECT * FROM technical_indicators WHERE symbol = ? ORDER BY ts ASC",
                ROW_MAPPER, symbol);
    }

    public Optional<TechnicalIndicator> findTopBySymbolOrderByDateDesc(String symbol) {
        List<TechnicalIndicator> results = jdbcTemplate.query(
                "SELECT * FROM technical_indicators LATEST ON ts PARTITION BY symbol WHERE symbol = ?",
                ROW_MAPPER, symbol);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<TechnicalIndicator> findBySymbolAndDate(String symbol, LocalDate date) {
        List<TechnicalIndicator> results = jdbcTemplate.query(
                "SELECT * FROM technical_indicators WHERE symbol = ? AND ts = ? LIMIT 1",
                ROW_MAPPER, symbol, Timestamp.valueOf(date.atStartOfDay()));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private static Double getDoubleOrNull(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }
}
