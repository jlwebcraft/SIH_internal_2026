package com.sih.supplychain.repository;

import com.sih.supplychain.domain.SupplierMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierMaterialRepository extends JpaRepository<SupplierMaterial, Long> {

    List<SupplierMaterial> findBySupplierId(Long supplierId);

    List<SupplierMaterial> findByMaterialId(Long materialId);

    Optional<SupplierMaterial> findBySupplierIdAndMaterialId(Long supplierId, Long materialId);
}
