package com.sih.supplychain.repository;

import com.sih.supplychain.domain.ProductMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductMaterialRepository extends JpaRepository<ProductMaterial, Long> {

    List<ProductMaterial> findByProductId(Long productId);

    List<ProductMaterial> findByMaterialId(Long materialId);
}
