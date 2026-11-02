package com.wise.account.exceptions;

import java.util.Currency;

public class NoBalanceForCurrencyException extends RuntimeException {

    private static final String MESSAGE = "No balance information was found for the currency %s on this account";

    public NoBalanceForCurrencyException(Currency currency) {
        super(MESSAGE.formatted(currency));
    }
}
