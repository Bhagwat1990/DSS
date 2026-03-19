package com.dss.repository;

import com.dss.entity.FeatureLabel;
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
public class FeatureLabelRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<FeatureLabel> ROW_MAPPER = (rs, rowNum) -> FeatureLabel.builder()
            .symbol(rs.getString("symbol"))
            .date(rs.getTimestamp("ts").toLocalDateTime().toLocalDate())
            .rsi(getDoubleOrNull(rs, "rsi"))
            .smaShortRatio(getDoubleOrNull(rs, "sma_short_ratio"))
            .smaLongRatio(getDoubleOrNull(rs, "sma_long_ratio"))
            .macd(getDoubleOrNull(rs, "macd"))
            .macdSignal(getDoubleOrNull(rs, "macd_signal"))
            .bollingerPosition(getDoubleOrNull(rs, "bollinger_position"))
            .priceChange(getDoubleOrNull(rs, "price_change"))
            .volumeChange(getDoubleOrNull(rs, "volume_change"))
            .atr(getDoubleOrNull(rs, "atr"))
            .label(rs.getInt("label"))
            .build();

    public void save(FeatureLabel fl) {
        jdbcTemplate.update(
                "INSERT INTO feature_labels(symbol, ts, rsi, sma_short_ratio, sma_long_ratio, macd, macd_signal, " +
                "bollinger_position, price_change, volume_change, atr, label) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                fl.getSymbol(), Timestamp.valueOf(fl.getDate().atStartOfDay()),
                fl.getRsi(), fl.getSmaShortRatio(), fl.getSmaLongRatio(),
                fl.getMacd(), fl.getMacdSignal(), fl.getBollingerPosition(),
                fl.getPriceChange(), fl.getVolumeChange(), fl.getAtr(), fl.getLabel());
    }

    public List<FeatureLabel> findBySymbolOrderByDateAsc(String symbol) {
        return jdbcTemplate.query(
                "SELECT * FROM feature_labels WHERE symbol = ? ORDER BY ts ASC",
                ROW_MAPPER, symbol);
    }

    public long countBySymbol(String symbol) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count() FROM feature_labels WHERE symbol = ?",
                Long.class, symbol);
        return count != null ? count : 0;
    }

    private static Double getDoubleOrNull(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }
}
