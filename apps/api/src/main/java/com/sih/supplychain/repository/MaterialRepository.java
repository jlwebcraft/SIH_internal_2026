package com.sih.supplychain.repository;

import com.sih.supplychain.domain.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByCode(String code);

    List<Material> findByStatus(String status);

    List<Material> findByCriticality(String criticality);

    boolean existsByCode(String code);
}
