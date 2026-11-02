package com.wise.quote;

import java.math.BigDecimal;

public record Quote(
        BigDecimal fee,
        BigDecimal convertedAmount,
        BigDecimal receivedAmount) {
}
