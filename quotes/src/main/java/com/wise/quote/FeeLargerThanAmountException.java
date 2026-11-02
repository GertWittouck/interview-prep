package com.wise.quote;

import java.math.BigDecimal;

public class FeeLargerThanAmountException extends RuntimeException {

    private static final String MESSAGE = "The fee %s cannot be greater than or equal to the amount %s";

    public FeeLargerThanAmountException(BigDecimal fee, BigDecimal amount) {
        super(MESSAGE.formatted(fee, amount));
    }
}
