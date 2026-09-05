package com.sih.supplychain.repository;

import com.sih.supplychain.domain.SupplierPerformance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SupplierPerformanceRepository extends JpaRepository<SupplierPerformance, Long> {

    @EntityGraph(attributePaths = {"supplier"})
    List<SupplierPerformance> findBySupplierId(Long supplierId);

    @EntityGraph(attributePaths = {"supplier"})
    List<SupplierPerformance> findBySupplierIdOrderByEvaluationDateDesc(Long supplierId);

    @EntityGraph(attributePaths = {"supplier"})
    Optional<SupplierPerformance> findBySupplierIdAndEvaluationDate(Long supplierId, LocalDate evaluationDate);

    @EntityGraph(attributePaths = {"supplier"})
    Optional<SupplierPerformance> findFirstBySupplierIdOrderByEvaluationDateDesc(Long supplierId);

    boolean existsBySupplierId(Long supplierId);
}
