package com.sih.supplychain.controller;

import com.sih.supplychain.dto.suppliermaterial.SupplierMaterialCreateRequest;
import com.sih.supplychain.dto.suppliermaterial.SupplierMaterialResponse;
import com.sih.supplychain.dto.suppliermaterial.SupplierMaterialUpdateRequest;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.SupplierMaterialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
public class SupplierMaterialController {

    private final SupplierMaterialService supplierMaterialService;

    public SupplierMaterialController(SupplierMaterialService supplierMaterialService) {
        this.supplierMaterialService = supplierMaterialService;
    }

    @PostMapping("/api/suppliers/{supplierId}/materials/{materialId}")
    public ResponseEntity<SupplierMaterialResponse> createSupplierMaterial(
            @PathVariable Long supplierId,
            @PathVariable Long materialId,
            @Valid @RequestBody SupplierMaterialCreateRequest request
    ) {
        SupplierMaterialResponse response = OperationalMapper.toSupplierMaterialResponse(
                this.supplierMaterialService.createSupplierMaterial(
                        supplierId,
                        materialId,
                        OperationalMapper.toSupplierMaterial(request)
                )
        );
        return ResponseEntity.created(URI.create("/api/supplier-materials/" + response.id())).body(response);
    }

    @GetMapping("/api/suppliers/{supplierId}/materials")
    public List<SupplierMaterialResponse> listMaterialsSuppliedBySupplier(@PathVariable Long supplierId) {
        return this.supplierMaterialService.listMaterialsSuppliedBySupplier(supplierId)
                .stream()
                .map(OperationalMapper::toSupplierMaterialResponse)
                .toList();
    }

    @GetMapping("/api/materials/{materialId}/suppliers")
    public List<SupplierMaterialResponse> listSuppliersForMaterial(@PathVariable Long materialId) {
        return this.supplierMaterialService.listSuppliersForMaterial(materialId)
                .stream()
                .map(OperationalMapper::toSupplierMaterialResponse)
                .toList();
    }

    @GetMapping("/api/supplier-materials/{id}")
    public SupplierMaterialResponse getSupplierMaterial(@PathVariable Long id) {
        return OperationalMapper.toSupplierMaterialResponse(
                this.supplierMaterialService.getSupplierMaterialById(id)
        );
    }

    @PutMapping("/api/supplier-materials/{id}")
    public SupplierMaterialResponse updateSupplierMaterial(
            @PathVariable Long id,
            @Valid @RequestBody SupplierMaterialUpdateRequest request
    ) {
        return OperationalMapper.toSupplierMaterialResponse(
                this.supplierMaterialService.updateSupplierMaterial(
                        id,
                        OperationalMapper.toSupplierMaterial(request)
                )
        );
    }

    @DeleteMapping("/api/supplier-materials/{id}")
    public ResponseEntity<Void> removeSupplierMaterial(@PathVariable Long id) {
        this.supplierMaterialService.removeSupplierMaterial(id);
        return ResponseEntity.noContent().build();
    }
}
