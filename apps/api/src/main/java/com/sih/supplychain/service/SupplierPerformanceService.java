package com.sih.supplychain.service;

import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.domain.SupplierPerformance;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.SupplierPerformanceRepository;
import com.sih.supplychain.repository.SupplierRepository;
import com.sih.supplychain.service.SupplierPerformanceCalculatorService.RawPerformanceMetrics;
import com.sih.supplychain.service.SupplierRiskEngineService.RiskEvaluationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SupplierPerformanceService {

    private final SupplierRepository supplierRepository;
    private final SupplierPerformanceRepository supplierPerformanceRepository;
    private final SupplierPerformanceCalculatorService performanceCalculator;
    private final SupplierRiskEngineService riskEngine;

    public SupplierPerformanceService(
            SupplierRepository supplierRepository,
            SupplierPerformanceRepository supplierPerformanceRepository,
            SupplierPerformanceCalculatorService performanceCalculator,
            SupplierRiskEngineService riskEngine
    ) {
        this.supplierRepository = supplierRepository;
        this.supplierPerformanceRepository = supplierPerformanceRepository;
        this.performanceCalculator = performanceCalculator;
        this.riskEngine = riskEngine;
    }

    public RawPerformanceMetrics calculatePerformance(Long supplierId, LocalDate evaluationDate) {
        Supplier supplier = getSupplier(supplierId);
        return this.performanceCalculator.calculatePerformance(supplier, evaluationDate);
    }

    public RiskEvaluationResult evaluateRisk(Long supplierId, LocalDate evaluationDate) {
        Supplier supplier = getSupplier(supplierId);
        RawPerformanceMetrics metrics = this.performanceCalculator.calculatePerformance(supplier, evaluationDate);
        return this.riskEngine.evaluateRisk(supplier, metrics);
    }

    @Transactional
    public SupplierPerformance takeSnapshot(Long supplierId, LocalDate evaluationDate) {
        Supplier supplier = getSupplier(supplierId);
        LocalDate evalDate = evaluationDate == null ? LocalDate.now() : evaluationDate;

        RawPerformanceMetrics metrics = this.performanceCalculator.calculatePerformance(supplier, evalDate);
        RiskEvaluationResult riskResult = this.riskEngine.evaluateRisk(supplier, metrics);

        SupplierPerformance performance = this.supplierPerformanceRepository
                .findBySupplierIdAndEvaluationDate(supplierId, evalDate)
                .orElseGet(() -> new SupplierPerformance(supplier, evalDate));

        performance.setOnTimeDeliveryRate(metrics.onTimeDeliveryRate());
        performance.setAverageDelayDays(metrics.averageDelayDays());
        performance.setLeadTimeVariance(metrics.leadTimeVariance());
        performance.setFulfillmentRate(metrics.fulfillmentRate());
        performance.setRejectionRate(metrics.rejectionRate());
        performance.setCapacityUtilization(metrics.capacityUtilization());
        performance.setDisruptionCount(metrics.disruptionCount());
        performance.setOverallScore(riskResult.overallScore());

        return this.supplierPerformanceRepository.save(performance);
    }

    public List<SupplierPerformance> getHistoricalSnapshots(Long supplierId) {
        getSupplier(supplierId);
        return this.supplierPerformanceRepository.findBySupplierIdOrderByEvaluationDateDesc(supplierId);
    }

    private Supplier getSupplier(Long supplierId) {
        return this.supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
    }
}
