package com.sih.supplychain.repository;

import com.sih.supplychain.domain.SupplierPerformance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPerformanceRepository extends JpaRepository<SupplierPerformance, Long> {

    List<SupplierPerformance> findBySupplierId(Long supplierId);

    List<SupplierPerformance> findBySupplierIdOrderByEvaluationDateDesc(Long supplierId);

    boolean existsBySupplierId(Long supplierId);
}
