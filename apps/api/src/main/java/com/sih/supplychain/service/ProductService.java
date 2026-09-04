package com.sih.supplychain.service;

import com.sih.supplychain.domain.Product;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.CustomerOrderItemRepository;
import com.sih.supplychain.repository.ProductMaterialRepository;
import com.sih.supplychain.repository.ProductRepository;
import com.sih.supplychain.repository.ProductionOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMaterialRepository productMaterialRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final CustomerOrderItemRepository customerOrderItemRepository;

    public ProductService(
            ProductRepository productRepository,
            ProductMaterialRepository productMaterialRepository,
            ProductionOrderRepository productionOrderRepository,
            CustomerOrderItemRepository customerOrderItemRepository
    ) {
        this.productRepository = productRepository;
        this.productMaterialRepository = productMaterialRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.customerOrderItemRepository = customerOrderItemRepository;
    }

    @Transactional
    public Product createProduct(Product product) {
        validateProduct(product);
        if (this.productRepository.existsByCode(product.getCode())) {
            throw new DuplicateResourceException("Product code already exists: " + product.getCode());
        }
        return this.productRepository.save(product);
    }

    public Product getProductById(Long id) {
        return this.productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public Product getProductByCode(String code) {
        BusinessValidation.requireText(code, "Product code");
        return this.productRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with code: " + code));
    }

    public List<Product> getAllProducts() {
        return this.productRepository.findAll();
    }

    public List<Product> getProductsByStatus(String status) {
        BusinessValidation.requireText(status, "Product status");
        return this.productRepository.findByStatus(status);
    }

    @Transactional
    public Product updateProduct(Long id, Product changes) {
        Product product = getProductById(id);
        validateProduct(changes);
        if (!Objects.equals(product.getCode(), changes.getCode())
                && this.productRepository.existsByCode(changes.getCode())) {
            throw new DuplicateResourceException("Product code already exists: " + changes.getCode());
        }

        product.setCode(changes.getCode());
        product.setName(changes.getName());
        product.setDescription(changes.getDescription());
        product.setCategory(changes.getCategory());
        product.setUnitCost(changes.getUnitCost());
        product.setSellingPrice(changes.getSellingPrice());
        product.setProductionTimeHours(changes.getProductionTimeHours());
        product.setStatus(changes.getStatus());
        return this.productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        if (this.productMaterialRepository.existsByProductId(id)
                || this.productionOrderRepository.existsByProductId(id)
                || this.customerOrderItemRepository.existsByProductId(id)) {
            throw new InvalidBusinessStateException("Product has dependent operational records and cannot be deleted");
        }
        this.productRepository.delete(product);
    }

    @Transactional
    public Product deactivateProduct(Long id) {
        Product product = getProductById(id);
        product.setStatus("INACTIVE");
        return this.productRepository.save(product);
    }

    private void validateProduct(Product product) {
        if (product == null) {
            throw new InvalidBusinessStateException("Product is required");
        }
        BusinessValidation.requireText(product.getCode(), "Product code");
        BusinessValidation.requireText(product.getName(), "Product name");
        BusinessValidation.requireNonNegative(product.getUnitCost(), "Product unit cost");
        BusinessValidation.requireNonNegative(product.getSellingPrice(), "Product selling price");
        BusinessValidation.requireNonNegative(product.getProductionTimeHours(), "Product production time");
    }
}
