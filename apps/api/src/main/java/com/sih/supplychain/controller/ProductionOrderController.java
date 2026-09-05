package com.sih.supplychain.controller;

import com.sih.supplychain.domain.ProductionOrder;
import com.sih.supplychain.dto.productionorder.ProductionOrderCreateRequest;
import com.sih.supplychain.dto.productionorder.ProductionOrderResponse;
import com.sih.supplychain.dto.productionorder.ProductionOrderStatusUpdateRequest;
import com.sih.supplychain.dto.productionorder.ProductionOrderUpdateRequest;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.ProductionOrderService;
import com.sih.supplychain.service.ProductionOrderService.CreateProductionOrderCommand;
import com.sih.supplychain.service.ProductionOrderService.UpdateProductionOrderCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/production-orders")
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;

    public ProductionOrderController(ProductionOrderService productionOrderService) {
        this.productionOrderService = productionOrderService;
    }

    @PostMapping
    public ResponseEntity<ProductionOrderResponse> createProductionOrder(
            @Valid @RequestBody ProductionOrderCreateRequest request
    ) {
        CreateProductionOrderCommand command = new CreateProductionOrderCommand(
                request.productionNumber(),
                request.productId(),
                request.quantity(),
                request.plannedStartDate(),
                request.plannedEndDate(),
                request.actualStartDate(),
                request.actualEndDate(),
                request.priority(),
                request.createdBy()
        );
        ProductionOrder created = this.productionOrderService.createProductionOrder(command);
        return ResponseEntity
                .created(URI.create("/api/production-orders/" + created.getId()))
                .body(OperationalMapper.toProductionOrderResponse(created));
    }

    @GetMapping("/{id}")
    public ProductionOrderResponse getProductionOrderById(@PathVariable Long id) {
        return OperationalMapper.toProductionOrderResponse(this.productionOrderService.getProductionOrderById(id));
    }

    @GetMapping("/by-number/{productionNumber}")
    public ProductionOrderResponse getProductionOrderByNumber(@PathVariable String productionNumber) {
        return OperationalMapper.toProductionOrderResponse(this.productionOrderService.getProductionOrderByNumber(productionNumber));
    }

    @GetMapping
    public List<ProductionOrderResponse> getProductionOrders(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String status
    ) {
        List<ProductionOrder> orders;
        if (productId != null) {
            orders = this.productionOrderService.getProductionOrdersByProduct(productId);
        } else if (status != null && !status.isBlank()) {
            orders = this.productionOrderService.getProductionOrdersByStatus(status);
        } else {
            orders = this.productionOrderService.getAllProductionOrders();
        }
        return orders.stream()
                .map(OperationalMapper::toProductionOrderResponse)
                .toList();
    }

    @PutMapping("/{id}")
    public ProductionOrderResponse updateProductionOrder(
            @PathVariable Long id,
            @Valid @RequestBody ProductionOrderUpdateRequest request
    ) {
        UpdateProductionOrderCommand command = new UpdateProductionOrderCommand(
                request.quantity(),
                request.plannedStartDate(),
                request.plannedEndDate(),
                request.actualStartDate(),
                request.actualEndDate(),
                request.priority(),
                request.createdBy()
        );
        ProductionOrder updated = this.productionOrderService.updateProductionOrder(id, command);
        return OperationalMapper.toProductionOrderResponse(updated);
    }

    @PutMapping("/{id}/status")
    public ProductionOrderResponse updateProductionOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductionOrderStatusUpdateRequest request
    ) {
        ProductionOrder updated = this.productionOrderService.updateStatus(id, request.status());
        return OperationalMapper.toProductionOrderResponse(updated);
    }

    @PatchMapping("/{id}/status")
    public ProductionOrderResponse patchProductionOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductionOrderStatusUpdateRequest request
    ) {
        ProductionOrder updated = this.productionOrderService.updateStatus(id, request.status());
        return OperationalMapper.toProductionOrderResponse(updated);
    }

    @PostMapping("/{id}/cancel")
    public ProductionOrderResponse cancelProductionOrder(@PathVariable Long id) {
        ProductionOrder cancelled = this.productionOrderService.cancelProductionOrder(id);
        return OperationalMapper.toProductionOrderResponse(cancelled);
    }

    @PatchMapping("/{id}/cancel")
    public ProductionOrderResponse patchCancelProductionOrder(@PathVariable Long id) {
        ProductionOrder cancelled = this.productionOrderService.cancelProductionOrder(id);
        return OperationalMapper.toProductionOrderResponse(cancelled);
    }
}
