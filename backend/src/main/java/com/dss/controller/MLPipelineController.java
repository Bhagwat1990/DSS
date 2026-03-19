package com.dss.controller;

import com.dss.dto.FeatureEngineeringRequest;
import com.dss.dto.FeatureEngineeringResponse;
import com.dss.dto.ModelTrainRequest;
import com.dss.dto.ModelTrainResponse;
import com.dss.service.FeatureEngineeringService;
import com.dss.service.ModelTrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for ML pipeline (Phase 2 & 3: Feature Engineering + Model Training).
 */
@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MLPipelineController {

    private final FeatureEngineeringService featureEngineeringService;
    private final ModelTrainingService modelTrainingService;

    /**
     * POST /api/ml/features
     * Generate features and labels from stock data + technical indicators.
     *
     * Request body:
     * {
     *   "symbol": "RELIANCE.BSE"
     * }
     */
    @PostMapping("/features")
    public ResponseEntity<FeatureEngineeringResponse> generateFeatures(
            @RequestBody FeatureEngineeringRequest request) {
        FeatureEngineeringResponse response = featureEngineeringService.generateFeatures(request.getSymbol());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/ml/train
     * Train the ML classifier on generated features.
     *
     * Request body:
     * {
     *   "symbol": "RELIANCE.BSE"
     * }
     */
    @PostMapping("/train")
    public ResponseEntity<ModelTrainResponse> trainModel(@RequestBody ModelTrainRequest request) {
        ModelTrainResponse response = modelTrainingService.trainModel(request.getSymbol());
        return ResponseEntity.ok(response);
    }
}
