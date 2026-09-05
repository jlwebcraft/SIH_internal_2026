package com.sih.supplychain.domain;

import java.math.BigDecimal;

public enum RiskBand {
    LOW(BigDecimal.ZERO, new BigDecimal("24.99")),
    MEDIUM(new BigDecimal("25.00"), new BigDecimal("49.99")),
    HIGH(new BigDecimal("50.00"), new BigDecimal("74.99")),
    CRITICAL(new BigDecimal("75.00"), new BigDecimal("100.00"));

    private final BigDecimal minScore;
    private final BigDecimal maxScore;

    RiskBand(BigDecimal minScore, BigDecimal maxScore) {
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public static RiskBand fromScore(BigDecimal score) {
        if (score == null) {
            return MEDIUM;
        }
        if (score.compareTo(new BigDecimal("25.00")) < 0) {
            return LOW;
        } else if (score.compareTo(new BigDecimal("50.00")) < 0) {
            return MEDIUM;
        } else if (score.compareTo(new BigDecimal("75.00")) < 0) {
            return HIGH;
        } else {
            return CRITICAL;
        }
    }
}
