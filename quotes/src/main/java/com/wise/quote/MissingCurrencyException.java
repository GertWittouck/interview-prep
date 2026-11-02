package com.wise.quote;

public class MissingCurrencyException extends RuntimeException {

    private static final String MESSAGE = "The currency value is missing";

    public MissingCurrencyException() {
        super(MESSAGE);
    }
}
