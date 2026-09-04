package com.sih.supplychain.controller;

import com.sih.supplychain.dto.product.ProductCreateRequest;
import com.sih.supplychain.dto.product.ProductResponse;
import com.sih.supplychain.dto.product.ProductUpdateRequest;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.ProductService;
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
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> listProducts(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String status
    ) {
        if (hasText(code)) {
            return List.of(OperationalMapper.toProductResponse(this.productService.getProductByCode(code)));
        }
        if (hasText(status)) {
            return this.productService.getProductsByStatus(status)
                    .stream()
                    .map(OperationalMapper::toProductResponse)
                    .toList();
        }
        return this.productService.getAllProducts()
                .stream()
                .map(OperationalMapper::toProductResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return OperationalMapper.toProductResponse(this.productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = OperationalMapper.toProductResponse(
                this.productService.createProduct(OperationalMapper.toProduct(request))
        );
        return ResponseEntity.created(URI.create("/api/products/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return OperationalMapper.toProductResponse(
                this.productService.updateProduct(id, OperationalMapper.toProduct(request))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        this.productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
