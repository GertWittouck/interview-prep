package com.wise.breaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircuitBreakerTest {

    private static final String HOST_B1 = "service-b-1";
    private static final String HOST_B2 = "service-b-2";
    private static final String HOST_C1 = "service-c-1";

    private Clock clock;
    private CircuitBreaker testable;

    @BeforeEach
    void setUp() {
        clock = new Clock();
        testable = new CircuitBreaker(clock);
    }

    @Nested
    class ClosedCircuitTest {

        @Test
        void returnsResponseWhenHostIsHealthy() {
            Response response = testable.execute(request(HOST_B1, CircuitBreakerTest::ok));

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo("ok");
        }

        @Test
        void returnsServerErrorResponseToCaller() {
            Response response = testable.execute(request(HOST_B1, CircuitBreakerTest::serverError));

            assertThat(response.getStatus()).isEqualTo(500);
        }

        @Test
        void rethrowsTimeout() {
            assertThatThrownBy(() -> testable.execute(request(HOST_B1, () -> { throw new ServiceTimeoutException(HOST_B1); })))
                    .isInstanceOf(ServiceTimeoutException.class);
        }

        @Test
        void twoFailuresKeepCircuitClosed() {
            failAt(0, HOST_B1);
            failAt(1, HOST_B1);

            assertThat(executeOk(HOST_B1).getStatus()).isEqualTo(200);
        }

        @Test
        void failuresSpreadOverTenMinutesKeepCircuitClosed() {
            failAt(0, HOST_B1);
            failAt(5, HOST_B1);
            failAt(10, HOST_B1);   // the failure at minute 0 is now 10 minutes old, so it no longer counts

            assertThat(executeOk(HOST_B1).getStatus()).isEqualTo(200);
        }

        @Test
        void clientErrorsDoNotCount() {
            for (int i = 0; i < 3; i++) {
                testable.execute(request(HOST_B1, () -> new Response(400, "bad request")));
            }

            assertThat(executeOk(HOST_B1).getStatus()).isEqualTo(200);
        }
    }

    @Nested
    class OpenCircuitTest {

        @Test
        void threeFailuresWithinTenMinutesOpenCircuit() {
            failAt(0, HOST_B1);
            failAt(4, HOST_B1);
            failAt(9, HOST_B1);

            AtomicInteger calls = new AtomicInteger();
            assertThatThrownBy(() -> testable.execute(request(HOST_B1, () -> {
                calls.incrementAndGet();
                return ok();
            }))).isInstanceOf(CircuitOpenException.class);
            assertThat(calls).hasValue(0);   // the host is not called while the circuit is open
        }

        @Test
        void timeoutsCountAsFailures() {
            failAt(0, HOST_B1);
            timeOutAt(1, HOST_B1);
            timeOutAt(2, HOST_B1);

            assertThatThrownBy(() -> executeOk(HOST_B1))
                    .isInstanceOf(CircuitOpenException.class);
        }

        @Test
        void circuitStaysOpenForFiveMinutes() {
            openCircuitAt(4, HOST_B1);

            clock.setTimeInMinutes(8);
            assertThatThrownBy(() -> executeOk(HOST_B1))
                    .isInstanceOf(CircuitOpenException.class);
        }

        @Test
        void callsAreAllowedAgainAfterFiveMinutes() {
            openCircuitAt(4, HOST_B1);

            clock.setTimeInMinutes(9);
            assertThat(executeOk(HOST_B1).getStatus()).isEqualTo(200);
        }

        @Test
        void oneFailureAfterReopeningDoesNotOpenCircuitAgain() {
            openCircuitAt(4, HOST_B1);

            failAt(9, HOST_B1);

            assertThat(executeOk(HOST_B1).getStatus()).isEqualTo(200);
        }

        @Test
        void otherHostsKeepWorking() {
            openCircuitAt(4, HOST_B1);

            assertThat(executeOk(HOST_B2).getStatus()).isEqualTo(200);
            assertThat(executeOk(HOST_C1).getStatus()).isEqualTo(200);
        }
    }

    private Response executeOk(String host) {
        return testable.execute(request(host, CircuitBreakerTest::ok));
    }

    private void failAt(int minute, String host) {
        clock.setTimeInMinutes(minute);
        assertThat(testable.execute(request(host, CircuitBreakerTest::serverError)).getStatus()).isEqualTo(500);
    }

    private void timeOutAt(int minute, String host) {
        clock.setTimeInMinutes(minute);
        assertThatThrownBy(() -> testable.execute(request(host, () -> { throw new ServiceTimeoutException(host); })))
                .isInstanceOf(ServiceTimeoutException.class);
    }

    private void openCircuitAt(int minute, String host) {
        failAt(minute, host);
        failAt(minute, host);
        failAt(minute, host);
    }

    private static Response ok() {
        return new Response(200, "ok");
    }

    private static Response serverError() {
        return new Response(500, "internal server error");
    }

    private static Request request(String host, Supplier<Response> behaviour) {
        return new Request(host) {
            @Override
            public Response call() {
                return behaviour.get();
            }
        };
    }
}
