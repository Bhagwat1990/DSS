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
            .build();

    public void save(PredictionResult pr) {
        jdbcTemplate.update(
                "INSERT INTO prediction_results(symbol, prediction_time, ml_prediction, ml_confidence, " +
                "signal, ai_suggestion, input_summary) VALUES (?, ?, ?, ?, ?, ?, ?)",
                pr.getSymbol(), Timestamp.valueOf(pr.getPredictionTime()),
                pr.getMlPrediction(), pr.getMlConfidence(),
                pr.getSignal(), pr.getAiSuggestion(), pr.getInputSummary());
    }

    public List<PredictionResult> findBySymbolOrderByPredictionTimeDesc(String symbol) {
        return jdbcTemplate.query(
                "SELECT * FROM prediction_results WHERE symbol = ? ORDER BY prediction_time DESC",
                ROW_MAPPER, symbol);
    }

    public PredictionResult findTopBySymbolOrderByPredictionTimeDesc(String symbol) {
        List<PredictionResult> results = jdbcTemplate.query(
                "SELECT * FROM prediction_results LATEST ON prediction_time PARTITION BY symbol WHERE symbol = ?",
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
}
