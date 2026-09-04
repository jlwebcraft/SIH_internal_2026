package com.sih.supplychain.dto.supplier;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SupplierUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 80) String code,
        @Size(max = 150) String contactPerson,
        @Email @Size(max = 320) String email,
        @Size(max = 50) String phone,
        @Size(max = 500) String address,
        @Size(max = 120) String city,
        @Size(max = 120) String state,
        @Size(max = 120) String country,
        @PositiveOrZero Integer leadTimeDays,
        @PositiveOrZero BigDecimal capacity,
        @PositiveOrZero @DecimalMax("100.00") BigDecimal reliabilityScore,
        @Size(max = 50) String status
) {
}
