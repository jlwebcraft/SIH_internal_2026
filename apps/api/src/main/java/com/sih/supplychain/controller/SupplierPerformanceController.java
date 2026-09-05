package com.sih.supplychain.controller;

import com.sih.supplychain.domain.SupplierPerformance;
import com.sih.supplychain.dto.performance.SupplierPerformanceResponse;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.SupplierPerformanceCalculatorService.RawPerformanceMetrics;
import com.sih.supplychain.service.SupplierPerformanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers/{supplierId}/performance")
public class SupplierPerformanceController {

    private final SupplierPerformanceService performanceService;

    public SupplierPerformanceController(SupplierPerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping
    public SupplierPerformanceResponse getCalculatedPerformance(
            @PathVariable Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate evaluationDate
    ) {
        RawPerformanceMetrics metrics = this.performanceService.calculatePerformance(supplierId, evaluationDate);
        return OperationalMapper.toPerformanceResponse(metrics);
    }

    @GetMapping("/history")
    public List<SupplierPerformanceResponse> getPerformanceHistory(@PathVariable Long supplierId) {
        List<SupplierPerformance> snapshots = this.performanceService.getHistoricalSnapshots(supplierId);
        return snapshots.stream()
                .map(OperationalMapper::toPerformanceResponse)
                .toList();
    }

    @PostMapping("/snapshot")
    public ResponseEntity<SupplierPerformanceResponse> createPerformanceSnapshot(
            @PathVariable Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate evaluationDate
    ) {
        SupplierPerformance snapshot = this.performanceService.takeSnapshot(supplierId, evaluationDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(OperationalMapper.toPerformanceResponse(snapshot));
    }
}
