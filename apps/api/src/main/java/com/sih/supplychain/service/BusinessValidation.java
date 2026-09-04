package com.sih.supplychain.service;

import com.sih.supplychain.exception.InvalidBusinessStateException;

import java.math.BigDecimal;

final class BusinessValidation {

    static final BigDecimal MAX_PERCENTAGE = new BigDecimal("100.00");

    private BusinessValidation() {
    }

    static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidBusinessStateException(fieldName + " is required");
        }
    }

    static void requireNonNegative(BigDecimal value, String fieldName) {
        if (value != null && value.signum() < 0) {
            throw new InvalidBusinessStateException(fieldName + " cannot be negative");
        }
    }

    static void requireNonNegative(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw new InvalidBusinessStateException(fieldName + " cannot be negative");
        }
    }

    static void requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.signum() <= 0) {
            throw new InvalidBusinessStateException(fieldName + " must be greater than zero");
        }
    }

    static void requirePercentageRange(BigDecimal value, String fieldName) {
        requirePercentageRange(value, fieldName, MAX_PERCENTAGE);
    }

    static void requirePercentageRange(BigDecimal value, String fieldName, BigDecimal maximum) {
        if (value != null && (value.signum() < 0 || value.compareTo(maximum) > 0)) {
            throw new InvalidBusinessStateException(fieldName + " must be between 0 and " + maximum);
        }
    }
}
