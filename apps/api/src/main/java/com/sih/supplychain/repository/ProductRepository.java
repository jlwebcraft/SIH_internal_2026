package com.sih.supplychain.repository;

import com.sih.supplychain.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByCode(String code);

    List<Product> findByStatus(String status);

    boolean existsByCode(String code);
}
