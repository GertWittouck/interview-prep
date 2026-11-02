package com.wise.breaker;

/**
 * A call to one host, as provided in the interview. Subclasses make the actual request in {@link #call()}.
 */
public abstract class Request {

    private final String host;

    protected Request(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public abstract Response call();
}
