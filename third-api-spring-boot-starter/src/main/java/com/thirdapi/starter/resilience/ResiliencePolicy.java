package com.thirdapi.starter.resilience;

import com.thirdapi.starter.config.ApiConfig;
import com.thirdapi.starter.http.HttpCallResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Lightweight retry and circuit breaker used by the invocation pipeline.
 */
public class ResiliencePolicy {

    private final ConcurrentMap<String, EndpointCircuitBreaker> breakers = new ConcurrentHashMap<String, EndpointCircuitBreaker>();

    public HttpCallResult execute(String key, ApiConfig config, Supplier<HttpCallResult> action) {
        EndpointCircuitBreaker breaker = breakers.computeIfAbsent(key,
                k -> new EndpointCircuitBreaker(
                        config.getCircuitBreakerThreshold(),
                        config.getCircuitBreakerMinCalls(),
                        config.getCircuitBreakerOpenTimeoutMs()));
        if (!breaker.isCallPermitted()) {
            HttpCallResult rejected = new HttpCallResult();
            rejected.setErrorType("CIRCUIT_OPEN");
            rejected.setErrorMessage("Circuit breaker is open for " + key);
            return rejected;
        }

        int attempts = Math.max(1, config.getMaxRetries() + 1);
        HttpCallResult last = null;
        for (int i = 0; i < attempts; i++) {
            last = action.get();
            if (last.isSuccess()) {
                breaker.recordSuccess();
                return last;
            }
            if (isRetryable(last) && i < attempts - 1) {
                sleep(config.getRetryBackoffMs() * (i + 1));
                continue;
            }
            breaker.recordFailure();
            return last;
        }
        breaker.recordFailure();
        return last;
    }

    private boolean isRetryable(HttpCallResult result) {
        return "IO".equals(result.getErrorType())
                || result.getStatusCode() == 429
                || result.getStatusCode() >= 500;
    }

    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class EndpointCircuitBreaker {

        private enum State {
            CLOSED, OPEN, HALF_OPEN
        }

        private final int failureThreshold;
        private final int minCalls;
        private final long openTimeoutMs;
        private State state = State.CLOSED;
        private long windowStart = System.currentTimeMillis();
        private int failures;
        private int successes;
        private long openUntil;

        private EndpointCircuitBreaker(int failureThreshold, int minCalls, long openTimeoutMs) {
            this.failureThreshold = failureThreshold;
            this.minCalls = Math.max(1, minCalls);
            this.openTimeoutMs = Math.max(1000L, openTimeoutMs);
        }

        private synchronized boolean isCallPermitted() {
            if (state == State.OPEN) {
                if (System.currentTimeMillis() >= openUntil) {
                    state = State.HALF_OPEN;
                    return true;
                }
                return false;
            }
            return true;
        }

        private synchronized void recordSuccess() {
            if (state == State.HALF_OPEN) {
                state = State.CLOSED;
                resetWindow();
                return;
            }
            successes++;
            if (successes + failures >= minCalls) {
                resetWindow();
            }
        }

        private synchronized void recordFailure() {
            if (state == State.HALF_OPEN) {
                state = State.OPEN;
                openUntil = System.currentTimeMillis() + openTimeoutMs;
                resetWindow();
                return;
            }
            failures++;
            if (successes + failures >= minCalls) {
                int total = successes + failures;
                int ratio = total == 0 ? 0 : failures * 100 / total;
                if (ratio >= failureThreshold) {
                    state = State.OPEN;
                    openUntil = System.currentTimeMillis() + openTimeoutMs;
                    resetWindow();
                }
            }
        }

        private void resetWindow() {
            windowStart = System.currentTimeMillis();
            failures = 0;
            successes = 0;
        }
    }
}
