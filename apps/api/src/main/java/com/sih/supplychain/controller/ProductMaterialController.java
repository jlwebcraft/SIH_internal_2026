package com.sih.supplychain.controller;

import com.sih.supplychain.dto.productmaterial.ProductMaterialCreateRequest;
import com.sih.supplychain.dto.productmaterial.ProductMaterialResponse;
import com.sih.supplychain.dto.productmaterial.ProductMaterialUpdateRequest;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.ProductMaterialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
public class ProductMaterialController {

    private final ProductMaterialService productMaterialService;

    public ProductMaterialController(ProductMaterialService productMaterialService) {
        this.productMaterialService = productMaterialService;
    }

    @PostMapping("/api/products/{productId}/bom/materials/{materialId}")
    public ResponseEntity<ProductMaterialResponse> addMaterialToProduct(
            @PathVariable Long productId,
            @PathVariable Long materialId,
            @Valid @RequestBody ProductMaterialCreateRequest request
    ) {
        ProductMaterialResponse response = OperationalMapper.toProductMaterialResponse(
                this.productMaterialService.addMaterialToProduct(
                        productId,
                        materialId,
                        OperationalMapper.toProductMaterial(request)
                )
        );
        return ResponseEntity.created(URI.create("/api/product-materials/" + response.id())).body(response);
    }

    @GetMapping("/api/products/{productId}/bom")
    public List<ProductMaterialResponse> getBomForProduct(@PathVariable Long productId) {
        return this.productMaterialService.getBomForProduct(productId)
                .stream()
                .map(OperationalMapper::toProductMaterialResponse)
                .toList();
    }

    @GetMapping("/api/materials/{materialId}/products")
    public List<ProductMaterialResponse> findProductsUsingMaterial(@PathVariable Long materialId) {
        return this.productMaterialService.findProductsUsingMaterial(materialId)
                .stream()
                .map(OperationalMapper::toProductMaterialResponse)
                .toList();
    }

    @GetMapping("/api/product-materials/{id}")
    public ProductMaterialResponse getBomEntry(@PathVariable Long id) {
        return OperationalMapper.toProductMaterialResponse(this.productMaterialService.getBomEntryById(id));
    }

    @PutMapping("/api/product-materials/{id}")
    public ProductMaterialResponse updateBomEntry(
            @PathVariable Long id,
            @Valid @RequestBody ProductMaterialUpdateRequest request
    ) {
        return OperationalMapper.toProductMaterialResponse(
                this.productMaterialService.updateBomEntry(id, OperationalMapper.toProductMaterial(request))
        );
    }

    @DeleteMapping("/api/product-materials/{id}")
    public ResponseEntity<Void> removeBomEntry(@PathVariable Long id) {
        this.productMaterialService.removeBomEntry(id);
        return ResponseEntity.noContent().build();
    }
}
