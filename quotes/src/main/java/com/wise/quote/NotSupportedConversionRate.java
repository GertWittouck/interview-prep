package com.wise.quote;

import java.util.Currency;

public class NotSupportedConversionRate extends RuntimeException {

    private static final String MESSAGE = "The conversion rate from %s to %s is not supported";

    public NotSupportedConversionRate(Currency from, Currency to) {
        super(MESSAGE.formatted(from, to));
    }
}
