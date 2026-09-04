package com.sih.supplychain.controller;

import com.sih.supplychain.dto.inventory.InventoryAdjustmentRequest;
import com.sih.supplychain.dto.inventory.InventoryCreateRequest;
import com.sih.supplychain.dto.inventory.InventoryResponse;
import com.sih.supplychain.dto.inventory.InventoryUpdateRequest;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.InventoryService;
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
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/inventory")
    public List<InventoryResponse> listInventory(
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String warehouseLocation
    ) {
        if (materialId != null && hasText(warehouseLocation)) {
            return List.of(OperationalMapper.toInventoryResponse(
                    this.inventoryService.getInventoryByMaterialAndWarehouse(materialId, warehouseLocation)
            ));
        }
        if (materialId != null) {
            return this.inventoryService.getInventoryForMaterial(materialId)
                    .stream()
                    .map(OperationalMapper::toInventoryResponse)
                    .toList();
        }
        if (hasText(warehouseLocation)) {
            return this.inventoryService.getInventoryByWarehouseLocation(warehouseLocation)
                    .stream()
                    .map(OperationalMapper::toInventoryResponse)
                    .toList();
        }
        return this.inventoryService.getAllInventory()
                .stream()
                .map(OperationalMapper::toInventoryResponse)
                .toList();
    }

    @GetMapping("/inventory/{id}")
    public InventoryResponse getInventory(@PathVariable Long id) {
        return OperationalMapper.toInventoryResponse(this.inventoryService.getInventoryById(id));
    }

    @GetMapping("/materials/{materialId}/inventory")
    public List<InventoryResponse> getInventoryForMaterial(@PathVariable Long materialId) {
        return this.inventoryService.getInventoryForMaterial(materialId)
                .stream()
                .map(OperationalMapper::toInventoryResponse)
                .toList();
    }

    @GetMapping("/materials/{materialId}/inventory/{warehouseLocation}")
    public InventoryResponse getInventoryByMaterialAndWarehouse(
            @PathVariable Long materialId,
            @PathVariable String warehouseLocation
    ) {
        return OperationalMapper.toInventoryResponse(
                this.inventoryService.getInventoryByMaterialAndWarehouse(materialId, warehouseLocation)
        );
    }

    @PostMapping("/inventory")
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody InventoryCreateRequest request) {
        InventoryResponse response = OperationalMapper.toInventoryResponse(
                this.inventoryService.createInventory(request.materialId(), OperationalMapper.toInventory(request))
        );
        return ResponseEntity.created(URI.create("/api/inventory/" + response.id())).body(response);
    }

    @PutMapping("/inventory/{id}")
    public InventoryResponse updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody InventoryUpdateRequest request
    ) {
        return OperationalMapper.toInventoryResponse(
                this.inventoryService.updateInventory(id, OperationalMapper.toInventory(request))
        );
    }

    @PatchMapping("/inventory/{id}/adjust")
    public InventoryResponse adjustInventory(
            @PathVariable Long id,
            @Valid @RequestBody InventoryAdjustmentRequest request
    ) {
        return OperationalMapper.toInventoryResponse(
                this.inventoryService.adjustStock(id, request.quantityChange())
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
