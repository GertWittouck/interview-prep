package com.wise.quote;

public class ZeroAmountException extends RuntimeException {

    private static final String MESSAGE = "The amount cannot be zero";

    public ZeroAmountException() {
        super(MESSAGE);
    }
}
