package com.sih.supplychain.dto.common;

public record UserSummaryResponse(
        Long id,
        String name,
        String email
) {
}
