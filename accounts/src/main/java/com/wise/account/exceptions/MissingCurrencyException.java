package com.wise.account.exceptions;

public class MissingCurrencyException extends RuntimeException {

    private static final String MESSAGE = "No currency was provided for this action %s";

    public MissingCurrencyException(String action) {
        super(MESSAGE.formatted(action));
    }
}
