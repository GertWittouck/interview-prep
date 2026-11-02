package com.wise.quote;

import java.util.Currency;

public class UnsupportedCurrencyException extends RuntimeException {

    private static final String MESSAGE = "Unsupported Currency %s";

    public UnsupportedCurrencyException(Currency currency) {
        super(MESSAGE.formatted(currency));
    }
}
