package com.wise.quote;

import java.math.BigDecimal;

public class NegativeAmountException extends RuntimeException {

    private static final String MESSAGE = "The amount %s cannot be negative";

    public NegativeAmountException(BigDecimal amount) {
        super(MESSAGE.formatted(amount));
    }
}
