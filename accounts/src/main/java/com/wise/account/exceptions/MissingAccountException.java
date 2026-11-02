package com.wise.account.exceptions;

public class MissingAccountException extends RuntimeException {

    private static final String MESSAGE = "No %s account was provided for this action %s";

    public MissingAccountException(String role, String action) {
        super(MESSAGE.formatted(role, action));
    }
}
