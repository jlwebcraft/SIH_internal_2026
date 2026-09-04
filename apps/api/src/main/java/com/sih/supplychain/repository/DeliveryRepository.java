package com.sih.supplychain.repository;

import com.sih.supplychain.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByPurchaseOrderId(Long purchaseOrderId);

    List<Delivery> findByTrackingNumber(String trackingNumber);
}
