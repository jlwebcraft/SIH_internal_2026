package com.sih.supplychain.repository;

import com.sih.supplychain.domain.ProductionOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"product", "createdBy"})
    List<ProductionOrder> findAll();

    @Override
    @EntityGraph(attributePaths = {"product", "createdBy"})
    Optional<ProductionOrder> findById(Long id);

    @EntityGraph(attributePaths = {"product", "createdBy"})
    Optional<ProductionOrder> findByProductionNumber(String productionNumber);

    @EntityGraph(attributePaths = {"product", "createdBy"})
    List<ProductionOrder> findByProductId(Long productId);

    @EntityGraph(attributePaths = {"product", "createdBy"})
    List<ProductionOrder> findByStatus(String status);

    boolean existsByProductId(Long productId);

    boolean existsByProductionNumber(String productionNumber);
}
