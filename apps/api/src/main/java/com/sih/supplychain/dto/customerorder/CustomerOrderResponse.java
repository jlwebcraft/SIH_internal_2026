package com.sih.supplychain.dto.customerorder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CustomerOrderResponse(
        Long id,
        String orderNumber,
        String customerName,
        LocalDate orderDate,
        LocalDate requiredDeliveryDate,
        String status,
        String priority,
        BigDecimal totalAmount,
        List<CustomerOrderItemResponse> items
) {
}
