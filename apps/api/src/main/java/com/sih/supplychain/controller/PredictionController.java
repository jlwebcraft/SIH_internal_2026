package com.sih.supplychain.controller;

import com.sih.supplychain.dto.prediction.DisruptionPredictionRequest;
import com.sih.supplychain.dto.prediction.DisruptionPredictionResponse;
import com.sih.supplychain.service.MlPredictionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final MlPredictionService predictionService;

    public PredictionController(MlPredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping(
            value = "/disruption",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public DisruptionPredictionResponse predictDisruption(
            @Valid @RequestBody DisruptionPredictionRequest request
    ) {
        return this.predictionService.predictDisruption(request);
    }
}
