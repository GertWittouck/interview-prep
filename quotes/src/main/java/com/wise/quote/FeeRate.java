package com.wise.quote;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record FeeRate(
        BigDecimal fixedRate,
        BigDecimal variableRatePercentage) {

    public BigDecimal variableRate() {
        return variableRatePercentage.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    }
}
