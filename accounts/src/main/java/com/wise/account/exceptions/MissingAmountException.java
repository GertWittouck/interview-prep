package com.wise.account.exceptions;

public class MissingAmountException extends RuntimeException {

    private static final String MESSAGE = "No amount was provided for this action %s";

    public MissingAmountException(String action) {
        super(MESSAGE.formatted(action));
    }
}
