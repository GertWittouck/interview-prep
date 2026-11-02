package com.wise.quote;

import java.math.BigDecimal;

public class InvalidFractionDigitsException extends RuntimeException {

    private static final String MESSAGE = "The amount %s can have at most %d fraction digits.";

    public InvalidFractionDigitsException(BigDecimal amount, int defaultFractionDigits) {
        super(MESSAGE.formatted(amount, defaultFractionDigits));
    }
}
