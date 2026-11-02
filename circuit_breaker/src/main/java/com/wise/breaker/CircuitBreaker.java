package com.wise.breaker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CircuitBreaker {

    private static final int MAX_FAILURES = 3;
    private static final int WINDOW_MINUTES = 10;
    private static final int OPEN_MINUTES = 5;

    private final Clock clock;
    private final Map<String, HostBreaker> breakers = new ConcurrentHashMap<>();

    public CircuitBreaker(Clock clock) {
        this.clock = clock;
    }

    public Response execute(Request request) {
        HostBreaker breaker = breakers.computeIfAbsent(request.getHost(), HostBreaker::new);
        breaker.checkAllowed(clock.getTimeInMinutes());

        try {
            Response response = request.call();
            if (isServerError(response)) {
                breaker.recordFailure(clock.getTimeInMinutes());
            }
            return response;
        } catch (ServiceTimeoutException e) {
            breaker.recordFailure(clock.getTimeInMinutes());
            throw e;
        }
    }

    private static boolean isServerError(Response response) {
        return response.getStatus() >= 500;
    }

    /**
     * Failure history and open/closed state for one host. Synchronized per host,
     * so a failing host never blocks calls to other hosts.
     */
    private static class HostBreaker {

        private final String host;
        private final Deque<Integer> failureTimes = new ArrayDeque<>();
        private Integer openUntil;

        HostBreaker(String host) {
            this.host = host;
        }

        synchronized void checkAllowed(int now) {
            if (openUntil != null && now < openUntil) {
                throw new CircuitOpenException(host, openUntil);
            }
            openUntil = null;
        }

        synchronized void recordFailure(int now) {
            failureTimes.addLast(now);
            failureTimes.removeIf(failureTime -> now - failureTime >= WINDOW_MINUTES);

            if (failureTimes.size() >= MAX_FAILURES) {
                openUntil = now + OPEN_MINUTES;
                failureTimes.clear();
            }
        }
    }
}
