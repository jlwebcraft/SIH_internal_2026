package com.sih.supplychain.controller;

import com.sih.supplychain.dto.supplier.SupplierCreateRequest;
import com.sih.supplychain.dto.supplier.SupplierResponse;
import com.sih.supplychain.dto.supplier.SupplierUpdateRequest;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<SupplierResponse> listSuppliers(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String status
    ) {
        if (hasText(code)) {
            return List.of(OperationalMapper.toSupplierResponse(this.supplierService.getSupplierByCode(code)));
        }
        if (hasText(status)) {
            return this.supplierService.getSuppliersByStatus(status)
                    .stream()
                    .map(OperationalMapper::toSupplierResponse)
                    .toList();
        }
        return this.supplierService.getAllSuppliers()
                .stream()
                .map(OperationalMapper::toSupplierResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public SupplierResponse getSupplier(@PathVariable Long id) {
        return OperationalMapper.toSupplierResponse(this.supplierService.getSupplierById(id));
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SupplierCreateRequest request) {
        SupplierResponse response = OperationalMapper.toSupplierResponse(
                this.supplierService.createSupplier(OperationalMapper.toSupplier(request))
        );
        return ResponseEntity.created(URI.create("/api/suppliers/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public SupplierResponse updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierUpdateRequest request
    ) {
        return OperationalMapper.toSupplierResponse(
                this.supplierService.updateSupplier(id, OperationalMapper.toSupplier(request))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        this.supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
