package com.sih.supplychain.repository;

import com.sih.supplychain.domain.Delivery;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    @Override
    @EntityGraph(attributePaths = {"purchaseOrder", "purchaseOrder.supplier"})
    List<Delivery> findAll();

    @Override
    @EntityGraph(attributePaths = {"purchaseOrder", "purchaseOrder.supplier"})
    Optional<Delivery> findById(Long id);

    @EntityGraph(attributePaths = {"purchaseOrder", "purchaseOrder.supplier"})
    List<Delivery> findByPurchaseOrderId(Long purchaseOrderId);

    @EntityGraph(attributePaths = {"purchaseOrder", "purchaseOrder.supplier"})
    List<Delivery> findByTrackingNumber(String trackingNumber);

    @EntityGraph(attributePaths = {"purchaseOrder", "purchaseOrder.supplier", "purchaseOrder.items", "purchaseOrder.items.material"})
    List<Delivery> findByPurchaseOrderSupplierId(Long supplierId);
}
