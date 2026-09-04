package com.sih.supplychain.service;

import com.sih.supplychain.domain.Product;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.repository.CustomerOrderItemRepository;
import com.sih.supplychain.repository.ProductMaterialRepository;
import com.sih.supplychain.repository.ProductRepository;
import com.sih.supplychain.repository.ProductionOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMaterialRepository productMaterialRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private CustomerOrderItemRepository customerOrderItemRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductSavesValidProduct() {
        Product product = new Product("PRD-001", "Motor Assembly");

        when(this.productRepository.existsByCode("PRD-001")).thenReturn(false);
        when(this.productRepository.save(product)).thenReturn(product);

        Product created = this.productService.createProduct(product);

        assertThat(created).isSameAs(product);
        verify(this.productRepository).save(product);
    }

    @Test
    void createProductRejectsDuplicateCode() {
        Product product = new Product("PRD-001", "Motor Assembly");
        when(this.productRepository.existsByCode("PRD-001")).thenReturn(true);

        assertThatThrownBy(() -> this.productService.createProduct(product))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
