package com.sih.supplychain.controller;

import com.sih.supplychain.dto.purchaseorder.PurchaseOrderCreateRequest;
import com.sih.supplychain.dto.purchaseorder.PurchaseOrderItemRequest;
import com.sih.supplychain.dto.purchaseorder.PurchaseOrderResponse;
import com.sih.supplychain.dto.purchaseorder.PurchaseOrderUpdateRequest;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.PurchaseOrderService;
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
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping
    public List<PurchaseOrderResponse> listPurchaseOrders(
            @RequestParam(required = false) String poNumber,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status
    ) {
        if (poNumber != null && !poNumber.isBlank()) {
            return List.of(OperationalMapper.toPurchaseOrderResponse(
                    this.purchaseOrderService.getPurchaseOrderByNumber(poNumber)
            ));
        }
        if (supplierId != null) {
            return this.purchaseOrderService.getPurchaseOrdersBySupplier(supplierId).stream()
                    .map(OperationalMapper::toPurchaseOrderResponse)
                    .toList();
        }
        if (status != null && !status.isBlank()) {
            return this.purchaseOrderService.getPurchaseOrdersByStatus(status).stream()
                    .map(OperationalMapper::toPurchaseOrderResponse)
                    .toList();
        }
        return this.purchaseOrderService.getAllPurchaseOrders().stream()
                .map(OperationalMapper::toPurchaseOrderResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse getPurchaseOrder(@PathVariable Long id) {
        return OperationalMapper.toPurchaseOrderResponse(this.purchaseOrderService.getPurchaseOrderById(id));
    }

    @GetMapping("/by-number/{poNumber}")
    public PurchaseOrderResponse getPurchaseOrderByNumber(@PathVariable String poNumber) {
        return OperationalMapper.toPurchaseOrderResponse(this.purchaseOrderService.getPurchaseOrderByNumber(poNumber));
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderCreateRequest request
    ) {
        PurchaseOrderResponse response = OperationalMapper.toPurchaseOrderResponse(
                this.purchaseOrderService.createPurchaseOrder(toCreateCommand(request))
        );
        return ResponseEntity
                .created(URI.create("/api/purchase-orders/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public PurchaseOrderResponse updatePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderUpdateRequest request
    ) {
        return OperationalMapper.toPurchaseOrderResponse(this.purchaseOrderService.updatePurchaseOrder(
                id,
                new PurchaseOrderService.UpdatePurchaseOrderCommand(request.expectedDeliveryDate())
        ));
    }

    @PatchMapping("/{id}/place")
    public PurchaseOrderResponse placePurchaseOrder(@PathVariable Long id) {
        return OperationalMapper.toPurchaseOrderResponse(this.purchaseOrderService.placePurchaseOrder(id));
    }

    @PatchMapping("/{id}/cancel")
    public PurchaseOrderResponse cancelPurchaseOrder(@PathVariable Long id) {
        return OperationalMapper.toPurchaseOrderResponse(this.purchaseOrderService.cancelPurchaseOrder(id));
    }

    private PurchaseOrderService.CreatePurchaseOrderCommand toCreateCommand(PurchaseOrderCreateRequest request) {
        return new PurchaseOrderService.CreatePurchaseOrderCommand(
                request.poNumber(),
                request.supplierId(),
                request.orderDate(),
                request.expectedDeliveryDate(),
                request.items().stream()
                        .map(this::toItemCommand)
                        .toList()
        );
    }

    private PurchaseOrderService.PurchaseOrderItemCommand toItemCommand(PurchaseOrderItemRequest request) {
        return new PurchaseOrderService.PurchaseOrderItemCommand(
                request.materialId(),
                request.quantity(),
                request.unitPrice(),
                request.expectedDate()
        );
    }
}
