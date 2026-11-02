package com.wise.breaker;

public class CircuitOpenException extends RuntimeException {

    private static final String MESSAGE = "Calls to host %s are paused until minute %d";

    public CircuitOpenException(String host, int openUntil) {
        super(MESSAGE.formatted(host, openUntil));
    }
}
