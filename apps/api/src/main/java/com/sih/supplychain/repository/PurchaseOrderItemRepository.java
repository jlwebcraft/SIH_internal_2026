package com.sih.supplychain.repository;

import com.sih.supplychain.domain.PurchaseOrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    @Override
    @EntityGraph(attributePaths = {"purchaseOrder", "material"})
    Optional<PurchaseOrderItem> findById(Long id);

    @EntityGraph(attributePaths = {"purchaseOrder", "material"})
    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);

    @EntityGraph(attributePaths = {"purchaseOrder", "material"})
    List<PurchaseOrderItem> findByMaterialId(Long materialId);

    boolean existsByMaterialId(Long materialId);
}
