package com.wise.account.exceptions;

public class SameAccountTransferException extends RuntimeException {

    private static final String MESSAGE = "Transfers from account %s to itself are not allowed";

    public SameAccountTransferException(long accountId) {
        super(MESSAGE.formatted(accountId));
    }
}
