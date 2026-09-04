package com.sih.supplychain.repository;

import com.sih.supplychain.domain.ProductMaterial;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductMaterialRepository extends JpaRepository<ProductMaterial, Long> {

    @Override
    @EntityGraph(attributePaths = {"product", "material"})
    Optional<ProductMaterial> findById(Long id);

    @EntityGraph(attributePaths = {"product", "material"})
    List<ProductMaterial> findByProductId(Long productId);

    @EntityGraph(attributePaths = {"product", "material"})
    List<ProductMaterial> findByMaterialId(Long materialId);

    @EntityGraph(attributePaths = {"product", "material"})
    Optional<ProductMaterial> findByProductIdAndMaterialId(Long productId, Long materialId);

    boolean existsByProductId(Long productId);

    boolean existsByMaterialId(Long materialId);

    boolean existsByProductIdAndMaterialId(Long productId, Long materialId);
}
