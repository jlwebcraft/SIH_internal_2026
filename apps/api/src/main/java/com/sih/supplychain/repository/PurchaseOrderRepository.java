package com.sih.supplychain.repository;

import com.sih.supplychain.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"supplier", "items", "items.material"})
    List<PurchaseOrder> findAll();

    @Override
    @EntityGraph(attributePaths = {"supplier", "items", "items.material"})
    Optional<PurchaseOrder> findById(Long id);

    @EntityGraph(attributePaths = {"supplier", "items", "items.material"})
    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    @EntityGraph(attributePaths = {"supplier", "items", "items.material"})
    List<PurchaseOrder> findBySupplierId(Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "items", "items.material"})
    List<PurchaseOrder> findByStatus(String status);

    boolean existsBySupplierId(Long supplierId);

    boolean existsByPoNumber(String poNumber);
}
