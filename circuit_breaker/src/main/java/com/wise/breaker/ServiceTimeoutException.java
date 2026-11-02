package com.wise.breaker;

public class ServiceTimeoutException extends RuntimeException {

    private static final String MESSAGE = "Call to host %s timed out";

    public ServiceTimeoutException(String host) {
        super(MESSAGE.formatted(host));
    }
}
