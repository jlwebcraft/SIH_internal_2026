package com.sih.supplychain.client.ml;

import com.sih.supplychain.exception.MlServiceException;
import com.sih.supplychain.exception.MlServiceTimeoutException;
import com.sih.supplychain.exception.MlServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

public class HttpMlPredictionClient implements MlPredictionClient {

    private static final Logger log = LoggerFactory.getLogger(HttpMlPredictionClient.class);
    private static final String PREDICT_PATH = "/api/predict/disruption";
    private static final String READY_PATH = "/api/ready";

    private final RestClient restClient;

    public HttpMlPredictionClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public FastApiPredictionResponse predictDisruption(FastApiPredictionRequest request) {
        try {
            FastApiPredictionResponse response = this.restClient.post()
                    .uri(PREDICT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(FastApiPredictionResponse.class);

            if (response == null) {
                throw new MlServiceException("Received empty response from ML prediction service");
            }

            return response;
        } catch (ResourceAccessException exception) {
            handleResourceAccessException(exception);
            throw new MlServiceUnavailableException("ML prediction service is currently unavailable", exception);
        } catch (RestClientResponseException exception) {
            handleRestClientResponseException(exception);
            throw new MlServiceException("ML prediction service returned an unexpected error", exception);
        } catch (Exception exception) {
            if (exception instanceof MlServiceUnavailableException || exception instanceof MlServiceException) {
                throw exception;
            }
            log.error("Unexpected error communicating with ML prediction service", exception);
            throw new MlServiceException("Failed to communicate with ML prediction service", exception);
        }
    }

    @Override
    public FastApiReadinessResponse checkReadiness() {
        try {
            return this.restClient.get()
                    .uri(READY_PATH)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FastApiReadinessResponse.class);
        } catch (ResourceAccessException exception) {
            handleResourceAccessException(exception);
            throw new MlServiceUnavailableException("ML prediction service readiness check unavailable", exception);
        } catch (RestClientResponseException exception) {
            handleRestClientResponseException(exception);
            throw new MlServiceException("ML prediction service readiness check returned an error", exception);
        } catch (Exception exception) {
            if (exception instanceof MlServiceUnavailableException || exception instanceof MlServiceException) {
                throw exception;
            }
            log.error("Unexpected error checking ML readiness", exception);
            throw new MlServiceException("Failed to check ML service readiness", exception);
        }
    }

    private void handleResourceAccessException(ResourceAccessException exception) {
        Throwable cause = exception.getCause();
        if (isTimeout(cause) || (exception.getMessage() != null && exception.getMessage().toLowerCase().contains("timeout"))) {
            log.warn("ML prediction service request timed out: {}", exception.getMessage());
            throw new MlServiceTimeoutException("ML prediction service request timed out", exception);
        }
        log.warn("ML prediction service connection failed: {}", exception.getMessage());
        throw new MlServiceUnavailableException("ML prediction service is currently unavailable", exception);
    }

    private void handleRestClientResponseException(RestClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();
        log.warn("ML prediction service returned HTTP {} {}", statusCode, exception.getStatusText());

        if (statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            throw new MlServiceUnavailableException("ML prediction service is currently unavailable or model is not loaded", exception);
        } else if (statusCode == HttpStatus.GATEWAY_TIMEOUT.value() || statusCode == HttpStatus.REQUEST_TIMEOUT.value()) {
            throw new MlServiceTimeoutException("ML prediction service timed out processing the request", exception);
        } else if (exception.getStatusCode().is4xxClientError()) {
            throw new MlServiceException("ML prediction service rejected the feature payload", exception);
        } else {
            throw new MlServiceException("ML prediction service failed with HTTP " + statusCode, exception);
        }
    }

    private boolean isTimeout(Throwable cause) {
        if (cause == null) {
            return false;
        }
        return cause instanceof SocketTimeoutException
                || cause instanceof TimeoutException
                || (cause.getMessage() != null && cause.getMessage().toLowerCase().contains("timeout"))
                || isTimeout(cause.getCause());
    }
}
