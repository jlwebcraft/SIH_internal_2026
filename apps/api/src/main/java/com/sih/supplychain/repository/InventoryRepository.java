package com.sih.supplychain.repository;

import com.sih.supplychain.domain.Inventory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Override
    @EntityGraph(attributePaths = "material")
    List<Inventory> findAll();

    @Override
    @EntityGraph(attributePaths = "material")
    Optional<Inventory> findById(Long id);

    @EntityGraph(attributePaths = "material")
    List<Inventory> findByMaterialId(Long materialId);

    @EntityGraph(attributePaths = "material")
    List<Inventory> findByWarehouseLocation(String warehouseLocation);

    @EntityGraph(attributePaths = "material")
    Optional<Inventory> findByMaterialIdAndWarehouseLocation(Long materialId, String warehouseLocation);

    boolean existsByMaterialId(Long materialId);

    boolean existsByMaterialIdAndWarehouseLocation(Long materialId, String warehouseLocation);
}
