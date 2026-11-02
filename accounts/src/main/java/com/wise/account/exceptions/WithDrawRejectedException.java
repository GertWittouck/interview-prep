package com.wise.account.exceptions;

import java.math.BigDecimal;
import java.util.Currency;

public class WithDrawRejectedException extends RuntimeException {

    private static final String MESSAGE = "Withdraw %s from the current balance %s (currency %s) is not allowed";

    public WithDrawRejectedException(BigDecimal amount, BigDecimal currentBalance, Currency currency) {
        super(MESSAGE.formatted(amount, currentBalance, currency));
    }
}
