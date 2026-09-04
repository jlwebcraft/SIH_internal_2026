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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductMaterialServiceTests {

    @Mock
    private ProductMaterialRepository productMaterialRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private ProductMaterialService productMaterialService;

    @Test
    void addMaterialToProductSavesValidBomEntry() {
        Product product = new Product("PRD-001", "Motor Assembly");
        Material material = new Material("MAT-001", "Steel Sheet");
        ProductMaterial details = new ProductMaterial(product, material, new BigDecimal("2.500"));

        when(this.productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(this.materialRepository.findById(2L)).thenReturn(Optional.of(material));
        when(this.productMaterialRepository.existsByProductIdAndMaterialId(1L, 2L)).thenReturn(false);
        when(this.productMaterialRepository.save(any(ProductMaterial.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductMaterial created = this.productMaterialService.addMaterialToProduct(1L, 2L, details);

        assertThat(created.getProduct()).isSameAs(product);
        assertThat(created.getMaterial()).isSameAs(material);
        assertThat(created.getQuantityRequired()).isEqualByComparingTo("2.500");
        verify(this.productMaterialRepository).save(any(ProductMaterial.class));
    }

    @Test
    void addMaterialToProductRejectsMissingProduct() {
        when(this.productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.productMaterialService.addMaterialToProduct(1L, 2L, validDetails()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addMaterialToProductRejectsMissingMaterial() {
        Product product = new Product("PRD-001", "Motor Assembly");
        when(this.productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(this.materialRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.productMaterialService.addMaterialToProduct(1L, 2L, validDetails()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addMaterialToProductRejectsDuplicateBomEntry() {
        Product product = new Product("PRD-001", "Motor Assembly");
        Material material = new Material("MAT-001", "Steel Sheet");
        when(this.productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(this.materialRepository.findById(2L)).thenReturn(Optional.of(material));
        when(this.productMaterialRepository.existsByProductIdAndMaterialId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> this.productMaterialService.addMaterialToProduct(1L, 2L, validDetails()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void addMaterialToProductRejectsInvalidQuantity() {
        Product product = new Product("PRD-001", "Motor Assembly");
        Material material = new Material("MAT-001", "Steel Sheet");
        ProductMaterial details = new ProductMaterial(product, material, BigDecimal.ZERO);

        when(this.productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(this.materialRepository.findById(2L)).thenReturn(Optional.of(material));
        when(this.productMaterialRepository.existsByProductIdAndMaterialId(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> this.productMaterialService.addMaterialToProduct(1L, 2L, details))
                .isInstanceOf(InvalidBusinessStateException.class);
    }

    private ProductMaterial validDetails() {
        return new ProductMaterial(
                new Product("PRD-001", "Motor Assembly"),
                new Material("MAT-001", "Steel Sheet"),
                new BigDecimal("2.500")
        );
    }
}
