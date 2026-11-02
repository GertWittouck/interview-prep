package com.wise.account.exceptions;

import java.math.BigDecimal;

public class NegativeAmountException extends RuntimeException {

    private static final String MESSAGE = "Negative amounts (%s) are not allowed for this action %s";

    public NegativeAmountException(BigDecimal amount, String action) {
        super(MESSAGE.formatted(amount, action));
    }
}
