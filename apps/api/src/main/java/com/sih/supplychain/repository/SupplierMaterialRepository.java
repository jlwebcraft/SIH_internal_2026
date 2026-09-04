package com.sih.supplychain.repository;

import com.sih.supplychain.domain.SupplierMaterial;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierMaterialRepository extends JpaRepository<SupplierMaterial, Long> {

    @Override
    @EntityGraph(attributePaths = {"supplier", "material"})
    Optional<SupplierMaterial> findById(Long id);

    @EntityGraph(attributePaths = {"supplier", "material"})
    List<SupplierMaterial> findBySupplierId(Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "material"})
    List<SupplierMaterial> findByMaterialId(Long materialId);

    @EntityGraph(attributePaths = {"supplier", "material"})
    Optional<SupplierMaterial> findBySupplierIdAndMaterialId(Long supplierId, Long materialId);

    boolean existsBySupplierId(Long supplierId);

    boolean existsByMaterialId(Long materialId);

    boolean existsBySupplierIdAndMaterialId(Long supplierId, Long materialId);
}
