package com.sih.supplychain.client.ml;

public interface MlPredictionClient {

    FastApiPredictionResponse predictDisruption(FastApiPredictionRequest request);

    FastApiReadinessResponse checkReadiness();
}
