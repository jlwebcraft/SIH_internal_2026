package com.sih.supplychain.dto.delivery;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DeliveryUpdateRequest(
        @Size(max = 120)
        String trackingNumber,

        LocalDate dispatchDate,

        LocalDate expectedArrivalDate,

        LocalDate actualArrivalDate,

        @PositiveOrZero
        Integer delayDays,

        @Size(max = 1000)
        String notes
) {
}
