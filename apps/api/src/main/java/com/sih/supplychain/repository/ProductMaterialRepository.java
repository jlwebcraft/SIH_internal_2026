package com.sih.supplychain.repository;

import com.sih.supplychain.domain.ProductMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductMaterialRepository extends JpaRepository<ProductMaterial, Long> {

    List<ProductMaterial> findByProductId(Long productId);

    List<ProductMaterial> findByMaterialId(Long materialId);

    Optional<ProductMaterial> findByProductIdAndMaterialId(Long productId, Long materialId);

    boolean existsByProductId(Long productId);

    boolean existsByMaterialId(Long materialId);

    boolean existsByProductIdAndMaterialId(Long productId, Long materialId);
}
