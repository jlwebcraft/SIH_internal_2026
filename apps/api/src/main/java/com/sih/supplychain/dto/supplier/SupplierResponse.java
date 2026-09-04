package com.sih.supplychain.dto.supplier;

import java.math.BigDecimal;
import java.time.Instant;

public record SupplierResponse(
        Long id,
        String name,
        String code,
        String contactPerson,
        String email,
        String phone,
        String address,
        String city,
        String state,
        String country,
        Integer leadTimeDays,
        BigDecimal capacity,
        BigDecimal reliabilityScore,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
