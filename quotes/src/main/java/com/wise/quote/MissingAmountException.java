package com.wise.quote;

public class MissingAmountException extends RuntimeException {

    private static final String MESSAGE = "The amount is missing";

    public MissingAmountException() {
        super(MESSAGE);
    }
}
