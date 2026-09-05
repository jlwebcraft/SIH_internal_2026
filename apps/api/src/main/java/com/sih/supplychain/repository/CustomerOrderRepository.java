package com.sih.supplychain.repository;

import com.sih.supplychain.domain.CustomerOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"items", "items.product"})
    List<CustomerOrder> findAll();

    @Override
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<CustomerOrder> findById(Long id);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<CustomerOrder> findByStatus(String status);

    boolean existsByOrderNumber(String orderNumber);
}
