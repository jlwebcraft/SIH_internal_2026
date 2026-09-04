package com.sih.supplychain.service;

import com.sih.supplychain.domain.Material;
import com.sih.supplychain.domain.Product;
import com.sih.supplychain.domain.ProductMaterial;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.MaterialRepository;
import com.sih.supplychain.repository.ProductMaterialRepository;
import com.sih.supplychain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductMaterialService {

    private static final BigDecimal MAX_WASTAGE_PERCENTAGE = new BigDecimal("100.00");

    private final ProductMaterialRepository productMaterialRepository;
    private final ProductRepository productRepository;
    private final MaterialRepository materialRepository;

    public ProductMaterialService(
            ProductMaterialRepository productMaterialRepository,
            ProductRepository productRepository,
            MaterialRepository materialRepository
    ) {
        this.productMaterialRepository = productMaterialRepository;
        this.productRepository = productRepository;
        this.materialRepository = materialRepository;
    }

    @Transactional
    public ProductMaterial addMaterialToProduct(Long productId, Long materialId, ProductMaterial details) {
        Product product = getProduct(productId);
        Material material = getMaterial(materialId);
        if (this.productMaterialRepository.existsByProductIdAndMaterialId(productId, materialId)) {
            throw new DuplicateResourceException("Product material BOM entry already exists");
        }
        validateDetails(details);

        ProductMaterial productMaterial = new ProductMaterial(product, material, details.getQuantityRequired());
        applyMutableFields(productMaterial, details);
        return this.productMaterialRepository.save(productMaterial);
    }

    public ProductMaterial getBomEntryById(Long id) {
        return this.productMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product material BOM entry not found with id: " + id));
    }

    public List<ProductMaterial> getBomForProduct(Long productId) {
        getProduct(productId);
        return this.productMaterialRepository.findByProductId(productId);
    }

    public List<ProductMaterial> findProductsUsingMaterial(Long materialId) {
        getMaterial(materialId);
        return this.productMaterialRepository.findByMaterialId(materialId);
    }

    @Transactional
    public ProductMaterial updateBomEntry(Long id, ProductMaterial changes) {
        ProductMaterial productMaterial = getBomEntryById(id);
        validateDetails(changes);
        applyMutableFields(productMaterial, changes);
        return this.productMaterialRepository.save(productMaterial);
    }

    @Transactional
    public void removeBomEntry(Long id) {
        ProductMaterial productMaterial = getBomEntryById(id);
        this.productMaterialRepository.delete(productMaterial);
    }

    private Product getProduct(Long productId) {
        return this.productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private Material getMaterial(Long materialId) {
        return this.materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialId));
    }

    private void validateDetails(ProductMaterial details) {
        if (details == null) {
            throw new InvalidBusinessStateException("Product material details are required");
        }
        BusinessValidation.requirePositive(details.getQuantityRequired(), "BOM quantity required");
        BusinessValidation.requirePercentageRange(
                details.getWastagePercentage(),
                "BOM wastage percentage",
                MAX_WASTAGE_PERCENTAGE
        );
    }

    private void applyMutableFields(ProductMaterial target, ProductMaterial source) {
        target.setQuantityRequired(source.getQuantityRequired());
        target.setUnit(source.getUnit());
        target.setWastagePercentage(source.getWastagePercentage());
    }
}
