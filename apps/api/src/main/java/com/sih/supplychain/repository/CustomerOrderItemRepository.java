package com.sih.supplychain.repository;

import com.sih.supplychain.domain.CustomerOrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerOrderItemRepository extends JpaRepository<CustomerOrderItem, Long> {

    @EntityGraph(attributePaths = {"product"})
    List<CustomerOrderItem> findByCustomerOrderId(Long customerOrderId);

    @EntityGraph(attributePaths = {"product"})
    List<CustomerOrderItem> findByProductId(Long productId);

    boolean existsByProductId(Long productId);
}
