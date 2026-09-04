package com.sih.supplychain.repository;

import com.sih.supplychain.domain.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    Optional<ProductionOrder> findByProductionNumber(String productionNumber);

    List<ProductionOrder> findByProductId(Long productId);

    List<ProductionOrder> findByStatus(String status);
}
