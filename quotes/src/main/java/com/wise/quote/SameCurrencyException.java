package com.wise.quote;

import java.util.Currency;

public class SameCurrencyException extends RuntimeException {

    private static final String MESSAGE = "Both the source and target currency (%s) are the same";

    public SameCurrencyException(Currency currency) {
        super(MESSAGE.formatted(currency));
    }
}
