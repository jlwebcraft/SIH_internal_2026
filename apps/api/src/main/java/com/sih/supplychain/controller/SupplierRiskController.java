package com.sih.supplychain.controller;

import com.sih.supplychain.dto.risk.SupplierRiskResponse;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.SupplierPerformanceService;
import com.sih.supplychain.service.SupplierRiskEngineService.RiskEvaluationResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/suppliers/{supplierId}/risk")
public class SupplierRiskController {

    private final SupplierPerformanceService performanceService;

    public SupplierRiskController(SupplierPerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping
    public SupplierRiskResponse getSupplierRisk(
            @PathVariable Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate evaluationDate
    ) {
        RiskEvaluationResult result = this.performanceService.evaluateRisk(supplierId, evaluationDate);
        return OperationalMapper.toRiskResponse(result);
    }
}
