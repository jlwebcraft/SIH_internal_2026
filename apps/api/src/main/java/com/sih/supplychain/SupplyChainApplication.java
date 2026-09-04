package com.sih.supplychain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SupplyChainApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupplyChainApplication.class, args);
    }

    @RestController
    static class HealthController {

        @GetMapping("/api/health")
        HealthResponse health() {
            return new HealthResponse("UP", "supply-chain-api");
        }
    }

    record HealthResponse(String status, String service) {
    }
}
