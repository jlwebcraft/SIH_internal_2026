package com.sih.supplychain.controller;

import com.sih.supplychain.dto.delivery.DeliveryCreateRequest;
import com.sih.supplychain.dto.delivery.DeliveryResponse;
import com.sih.supplychain.dto.delivery.DeliveryUpdateRequest;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/deliveries")
    public List<DeliveryResponse> listDeliveries(@RequestParam(required = false) Long purchaseOrderId) {
        if (purchaseOrderId != null) {
            return this.deliveryService.getDeliveriesForPurchaseOrder(purchaseOrderId).stream()
                    .map(OperationalMapper::toDeliveryResponse)
                    .toList();
        }
        return this.deliveryService.getAllDeliveries().stream()
                .map(OperationalMapper::toDeliveryResponse)
                .toList();
    }

    @GetMapping("/deliveries/{id}")
    public DeliveryResponse getDelivery(@PathVariable Long id) {
        return OperationalMapper.toDeliveryResponse(this.deliveryService.getDeliveryById(id));
    }

    @GetMapping("/purchase-orders/{purchaseOrderId}/deliveries")
    public List<DeliveryResponse> listDeliveriesForPurchaseOrder(@PathVariable Long purchaseOrderId) {
        return this.deliveryService.getDeliveriesForPurchaseOrder(purchaseOrderId).stream()
                .map(OperationalMapper::toDeliveryResponse)
                .toList();
    }

    @PostMapping("/purchase-orders/{purchaseOrderId}/deliveries")
    public ResponseEntity<DeliveryResponse> createDelivery(
            @PathVariable Long purchaseOrderId,
            @Valid @RequestBody DeliveryCreateRequest request
    ) {
        DeliveryResponse response = OperationalMapper.toDeliveryResponse(this.deliveryService.createDelivery(
                purchaseOrderId,
                OperationalMapper.toDelivery(request)
        ));
        return ResponseEntity
                .created(URI.create("/api/deliveries/" + response.id()))
                .body(response);
    }

    @PutMapping("/deliveries/{id}")
    public DeliveryResponse updateDelivery(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryUpdateRequest request
    ) {
        return OperationalMapper.toDeliveryResponse(this.deliveryService.updateDelivery(
                id,
                OperationalMapper.toDelivery(request)
        ));
    }

    @PatchMapping("/deliveries/{id}/dispatch")
    public DeliveryResponse dispatchDelivery(@PathVariable Long id) {
        return OperationalMapper.toDeliveryResponse(this.deliveryService.dispatchDelivery(id));
    }

    @PatchMapping("/deliveries/{id}/in-transit")
    public DeliveryResponse markInTransit(@PathVariable Long id) {
        return OperationalMapper.toDeliveryResponse(this.deliveryService.markInTransit(id));
    }

    @PatchMapping("/deliveries/{id}/delay")
    public DeliveryResponse markDelayed(@PathVariable Long id) {
        return OperationalMapper.toDeliveryResponse(this.deliveryService.markDelayed(id));
    }

    @PatchMapping("/deliveries/{id}/deliver")
    public DeliveryResponse markDelivered(@PathVariable Long id) {
        return OperationalMapper.toDeliveryResponse(this.deliveryService.markDelivered(id));
    }

    @PatchMapping("/deliveries/{id}/cancel")
    public DeliveryResponse cancelDelivery(@PathVariable Long id) {
        return OperationalMapper.toDeliveryResponse(this.deliveryService.cancelDelivery(id));
    }
}
