package com.sih.supplychain.config;

import com.sih.supplychain.client.ml.HttpMlPredictionClient;
import com.sih.supplychain.client.ml.MlPredictionClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class MlClientConfig {

    @Bean
    public MlPredictionClient mlPredictionClient(
            @Value("${ml.service.base-url:http://localhost:8000}") String baseUrl,
            @Value("${ml.service.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${ml.service.read-timeout-ms:5000}") long readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        return new HttpMlPredictionClient(restClient);
    }
}
