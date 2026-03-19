package com.dss.service;

import com.dss.dto.ModelTrainResponse;
import com.dss.entity.FeatureLabel;
import com.dss.repository.FeatureLabelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Phase 3: Model Training
 * Implements a simple Decision Tree / Random Forest-style classifier in pure Java.
 * For production, integrate with Python ML services (scikit-learn, XGBoost) via REST.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelTrainingService {

    private final FeatureLabelRepository featureLabelRepository;

    // In-memory model storage per symbol
    private final Map<String, TrainedModel> models = new HashMap<>();

    /**
     * Train a simple classifier on historical feature-label data.
     * Uses a weighted voting ensemble of threshold-based rules (mimics Random Forest logic).
     */
    public ModelTrainResponse trainModel(String symbol) {
        List<FeatureLabel> features = featureLabelRepository.findBySymbolOrderByDateAsc(symbol);

        if (features.size() < 30) {
            return ModelTrainResponse.builder()
                    .symbol(symbol)
                    .trainingRecords(features.size())
                    .accuracy(0.0)
                    .message("Insufficient training data. Need at least 30 records, have " + features.size())
                    .build();
        }

        // Split: 80% train, 20% test
        int trainSize = (int) (features.size() * 0.8);
        List<FeatureLabel> trainSet = features.subList(0, trainSize);
        List<FeatureLabel> testSet = features.subList(trainSize, features.size());

        // Calculate optimal thresholds from training data
        TrainedModel model = new TrainedModel();
        model.symbol = symbol;

        // Compute means for each feature where label=1 vs label=0
        double rsiUpMean = trainSet.stream().filter(f -> f.getLabel() == 1 && f.getRsi() != null)
                .mapToDouble(FeatureLabel::getRsi).average().orElse(50);
        double rsiDownMean = trainSet.stream().filter(f -> f.getLabel() == 0 && f.getRsi() != null)
                .mapToDouble(FeatureLabel::getRsi).average().orElse(50);
        model.rsiThreshold = (rsiUpMean + rsiDownMean) / 2;

        double macdUpMean = trainSet.stream().filter(f -> f.getLabel() == 1 && f.getMacd() != null)
                .mapToDouble(FeatureLabel::getMacd).average().orElse(0);
        double macdDownMean = trainSet.stream().filter(f -> f.getLabel() == 0 && f.getMacd() != null)
                .mapToDouble(FeatureLabel::getMacd).average().orElse(0);
        model.macdThreshold = (macdUpMean + macdDownMean) / 2;

        double smaRatioUpMean = trainSet.stream().filter(f -> f.getLabel() == 1 && f.getSmaShortRatio() != null)
                .mapToDouble(FeatureLabel::getSmaShortRatio).average().orElse(1.0);
        double smaRatioDownMean = trainSet.stream().filter(f -> f.getLabel() == 0 && f.getSmaShortRatio() != null)
                .mapToDouble(FeatureLabel::getSmaShortRatio).average().orElse(1.0);
        model.smaShortRatioThreshold = (smaRatioUpMean + smaRatioDownMean) / 2;

        double bollUpMean = trainSet.stream().filter(f -> f.getLabel() == 1 && f.getBollingerPosition() != null)
                .mapToDouble(FeatureLabel::getBollingerPosition).average().orElse(0.5);
        double bollDownMean = trainSet.stream().filter(f -> f.getLabel() == 0 && f.getBollingerPosition() != null)
                .mapToDouble(FeatureLabel::getBollingerPosition).average().orElse(0.5);
        model.bollingerThreshold = (bollUpMean + bollDownMean) / 2;

        // Learn vote directions
        model.rsiVoteUp = rsiUpMean > rsiDownMean;
        model.macdVoteUp = macdUpMean > macdDownMean;
        model.smaRatioVoteUp = smaRatioUpMean > smaRatioDownMean;
        model.bollingerVoteUp = bollUpMean > bollDownMean;

        model.trained = true;

        // Evaluate on test set
        int correct = 0;
        for (FeatureLabel f : testSet) {
            PredictionOutput pred = predict(model, f);
            if (pred.prediction == f.getLabel()) correct++;
        }
        double accuracy = (double) correct / testSet.size();
        model.lastAccuracy = accuracy;

        models.put(symbol, model);

        log.info("Model trained for {} with {} records. Test accuracy: {}%",
                symbol, trainSet.size(), Math.round(accuracy * 10000.0) / 100.0);

        return ModelTrainResponse.builder()
                .symbol(symbol)
                .trainingRecords(trainSet.size())
                .accuracy(Math.round(accuracy * 10000.0) / 100.0) // percentage with 2 decimals
                .message(String.format("Model trained successfully. Test accuracy: %.2f%% (%d/%d correct)",
                        accuracy * 100, correct, testSet.size()))
                .build();
    }

    /**
     * Predict using the trained model for a given feature set.
     */
    public PredictionOutput predictForSymbol(String symbol, FeatureLabel features) {
        TrainedModel model = models.get(symbol);
        if (model == null || !model.trained) {
            return new PredictionOutput(0, 0.5, false);
        }
        return predict(model, features);
    }

    public boolean isModelTrained(String symbol) {
        TrainedModel model = models.get(symbol);
        return model != null && model.trained;
    }

    private PredictionOutput predict(TrainedModel model, FeatureLabel f) {
        int votes = 0;
        int totalVotes = 0;

        // RSI vote
        if (f.getRsi() != null) {
            boolean aboveThreshold = f.getRsi() > model.rsiThreshold;
            boolean voteUp = model.rsiVoteUp ? aboveThreshold : !aboveThreshold;
            if (voteUp) votes++;
            totalVotes++;

            // Extra signal: oversold (RSI < 30) usually means buy
            if (f.getRsi() < 30) { votes++; totalVotes++; }
            if (f.getRsi() > 70) { totalVotes++; } // overbought, no vote for up
        }

        // MACD vote
        if (f.getMacd() != null) {
            boolean aboveThreshold = f.getMacd() > model.macdThreshold;
            boolean voteUp = model.macdVoteUp ? aboveThreshold : !aboveThreshold;
            if (voteUp) votes++;
            totalVotes++;
        }

        // SMA ratio vote
        if (f.getSmaShortRatio() != null) {
            boolean aboveThreshold = f.getSmaShortRatio() > model.smaShortRatioThreshold;
            boolean voteUp = model.smaRatioVoteUp ? aboveThreshold : !aboveThreshold;
            if (voteUp) votes++;
            totalVotes++;
        }

        // Bollinger position vote
        if (f.getBollingerPosition() != null) {
            boolean aboveThreshold = f.getBollingerPosition() > model.bollingerThreshold;
            boolean voteUp = model.bollingerVoteUp ? aboveThreshold : !aboveThreshold;
            if (voteUp) votes++;
            totalVotes++;
        }

        double confidence = totalVotes > 0 ? (double) votes / totalVotes : 0.5;
        int prediction = confidence >= 0.5 ? 1 : 0;

        return new PredictionOutput(prediction, confidence, true);
    }

    // Inner classes
    public static class PredictionOutput {
        public final int prediction;       // 1=up, 0=down
        public final double confidence;    // [0, 1]
        public final boolean modelAvailable;

        public PredictionOutput(int prediction, double confidence, boolean modelAvailable) {
            this.prediction = prediction;
            this.confidence = confidence;
            this.modelAvailable = modelAvailable;
        }
    }

    private static class TrainedModel {
        String symbol;
        boolean trained;
        double rsiThreshold;
        double macdThreshold;
        double smaShortRatioThreshold;
        double bollingerThreshold;
        boolean rsiVoteUp;
        boolean macdVoteUp;
        boolean smaRatioVoteUp;
        boolean bollingerVoteUp;
        double lastAccuracy;
    }
}
