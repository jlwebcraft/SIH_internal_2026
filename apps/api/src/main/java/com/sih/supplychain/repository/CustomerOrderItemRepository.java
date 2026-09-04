package com.sih.supplychain.repository;

import com.sih.supplychain.domain.CustomerOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerOrderItemRepository extends JpaRepository<CustomerOrderItem, Long> {

    List<CustomerOrderItem> findByCustomerOrderId(Long customerOrderId);

    List<CustomerOrderItem> findByProductId(Long productId);

    boolean existsByProductId(Long productId);
}
