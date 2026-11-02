package com.wise.account.exceptions;

import java.math.BigDecimal;

public class InvalidAmountException extends RuntimeException {

    private static final String MESSAGE = "Amounts (%s) with a precision different from 2 not allowed for this action %s";

    public InvalidAmountException(BigDecimal amount, String action) {
        super(MESSAGE.formatted(amount, action));
    }
}
