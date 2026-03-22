package com.dss.repository;

import com.dss.entity.PredictionResult;
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
public class PredictionResultRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<PredictionResult> ROW_MAPPER = (rs, rowNum) -> PredictionResult.builder()
            .symbol(rs.getString("symbol"))
            .predictionTime(rs.getTimestamp("prediction_time").toLocalDateTime())
            .mlPrediction(getIntOrNull(rs, "ml_prediction"))
            .mlConfidence(getDoubleOrNull(rs, "ml_confidence"))
            .signal(rs.getString("signal"))
            .aiSuggestion(rs.getString("ai_suggestion"))
            .inputSummary(rs.getString("input_summary"))
            .isDeleted(getBooleanOrNull(rs, "is_deleted"))
            .build();

    public void save(PredictionResult pr) {
        jdbcTemplate.update(
                "INSERT INTO prediction_results(symbol, prediction_time, ml_prediction, ml_confidence, " +
                "signal, ai_suggestion, input_summary, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                pr.getSymbol(), Timestamp.valueOf(pr.getPredictionTime()),
                pr.getMlPrediction(), pr.getMlConfidence(),
                pr.getSignal(), pr.getAiSuggestion(), pr.getInputSummary(),
                pr.getIsDeleted() != null && pr.getIsDeleted());
    }

    public List<PredictionResult> findBySymbolOrderByPredictionTimeDesc(String symbol) {
        return jdbcTemplate.query(
                "SELECT * FROM prediction_results WHERE symbol = ? AND (is_deleted != true OR is_deleted IS NULL) ORDER BY prediction_time DESC",
                ROW_MAPPER, symbol);
    }

    public PredictionResult findTopBySymbolOrderByPredictionTimeDesc(String symbol) {
        List<PredictionResult> results = jdbcTemplate.query(
                "SELECT * FROM prediction_results WHERE symbol = ? AND (is_deleted != true OR is_deleted IS NULL) LATEST ON prediction_time PARTITION BY symbol",
                ROW_MAPPER, symbol);
        return results.isEmpty() ? null : results.get(0);
    }

    private static Double getDoubleOrNull(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    private static Integer getIntOrNull(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private static Boolean getBooleanOrNull(ResultSet rs, String col) throws SQLException {
        boolean v = rs.getBoolean(col);
        return rs.wasNull() ? null : v;
    }
}
