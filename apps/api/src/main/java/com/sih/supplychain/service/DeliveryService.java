package com.sih.supplychain.service;

import com.sih.supplychain.domain.Delivery;
import com.sih.supplychain.domain.PurchaseOrder;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.DeliveryRepository;
import com.sih.supplychain.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DeliveryService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DISPATCHED = "DISPATCHED";
    public static final String STATUS_IN_TRANSIT = "IN_TRANSIT";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_DELAYED = "DELAYED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final DeliveryRepository deliveryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public DeliveryService(DeliveryRepository deliveryRepository, PurchaseOrderRepository purchaseOrderRepository) {
        this.deliveryRepository = deliveryRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Transactional
    public Delivery createDelivery(Long purchaseOrderId, Delivery details) {
        PurchaseOrder purchaseOrder = getPurchaseOrder(purchaseOrderId);
        validateDeliveryDetails(details);
        Delivery delivery = new Delivery(purchaseOrder, details.getTrackingNumber());
        applyMutableFields(delivery, details);
        delivery.setStatus(STATUS_PENDING);
        return this.deliveryRepository.save(delivery);
    }

    public Delivery getDeliveryById(Long id) {
        return this.deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with id: " + id));
    }

    public List<Delivery> getAllDeliveries() {
        return this.deliveryRepository.findAll();
    }

    public List<Delivery> getDeliveriesForPurchaseOrder(Long purchaseOrderId) {
        getPurchaseOrder(purchaseOrderId);
        return this.deliveryRepository.findByPurchaseOrderId(purchaseOrderId);
    }

    @Transactional
    public Delivery updateDelivery(Long id, Delivery changes) {
        Delivery delivery = getDeliveryById(id);
        validateDeliveryDetails(changes);
        applyMutableFields(delivery, changes);
        return this.deliveryRepository.save(delivery);
    }

    @Transactional
    public Delivery dispatchDelivery(Long id) {
        Delivery delivery = getDeliveryById(id);
        requireTransition(delivery.getStatus(), STATUS_DISPATCHED, STATUS_PENDING);
        delivery.setStatus(STATUS_DISPATCHED);
        return this.deliveryRepository.save(delivery);
    }

    @Transactional
    public Delivery markInTransit(Long id) {
        Delivery delivery = getDeliveryById(id);
        requireTransition(delivery.getStatus(), STATUS_IN_TRANSIT, STATUS_DISPATCHED, STATUS_DELAYED);
        delivery.setStatus(STATUS_IN_TRANSIT);
        return this.deliveryRepository.save(delivery);
    }

    @Transactional
    public Delivery markDelayed(Long id) {
        Delivery delivery = getDeliveryById(id);
        requireTransition(delivery.getStatus(), STATUS_DELAYED, STATUS_DISPATCHED, STATUS_IN_TRANSIT);
        delivery.setStatus(STATUS_DELAYED);
        return this.deliveryRepository.save(delivery);
    }

    @Transactional
    public Delivery markDelivered(Long id) {
        Delivery delivery = getDeliveryById(id);
        requireTransition(delivery.getStatus(), STATUS_DELIVERED, STATUS_DISPATCHED, STATUS_IN_TRANSIT, STATUS_DELAYED);
        delivery.setStatus(STATUS_DELIVERED);
        return this.deliveryRepository.save(delivery);
    }

    @Transactional
    public Delivery cancelDelivery(Long id) {
        Delivery delivery = getDeliveryById(id);
        requireTransition(delivery.getStatus(), STATUS_CANCELLED, STATUS_PENDING, STATUS_DISPATCHED, STATUS_IN_TRANSIT, STATUS_DELAYED);
        delivery.setStatus(STATUS_CANCELLED);
        return this.deliveryRepository.save(delivery);
    }

    private void validateDeliveryDetails(Delivery delivery) {
        if (delivery == null) {
            throw new InvalidBusinessStateException("Delivery details are required");
        }
        BusinessValidation.requireNonNegative(delivery.getDelayDays(), "Delivery delay days");
        validateDateRelationship(delivery.getDispatchDate(), delivery.getExpectedArrivalDate(), "Expected arrival date");
        validateDateRelationship(delivery.getDispatchDate(), delivery.getActualArrivalDate(), "Actual arrival date");
    }

    private void validateDateRelationship(LocalDate dispatchDate, LocalDate arrivalDate, String fieldName) {
        if (dispatchDate != null && arrivalDate != null && arrivalDate.isBefore(dispatchDate)) {
            throw new InvalidBusinessStateException(fieldName + " cannot be before dispatch date");
        }
    }

    private void applyMutableFields(Delivery target, Delivery source) {
        target.setTrackingNumber(source.getTrackingNumber());
        target.setDispatchDate(source.getDispatchDate());
        target.setExpectedArrivalDate(source.getExpectedArrivalDate());
        target.setActualArrivalDate(source.getActualArrivalDate());
        target.setDelayDays(source.getDelayDays());
        target.setNotes(source.getNotes());
    }

    private void requireTransition(String currentStatus, String targetStatus, String... allowedCurrentStatuses) {
        for (String allowedStatus : allowedCurrentStatuses) {
            if (allowedStatus.equals(currentStatus)) {
                return;
            }
        }
        throw new InvalidBusinessStateException(
                "Delivery cannot transition from " + currentStatus + " to " + targetStatus
        );
    }

    private PurchaseOrder getPurchaseOrder(Long purchaseOrderId) {
        return this.purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + purchaseOrderId));
    }
}
