package com.thirdapi.starter.resilience;

import com.thirdapi.starter.config.ApiConfig;
import com.thirdapi.starter.http.HttpCallResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * 调用链路使用的轻量级重试与熔断策略。
 *
 * <p>每个端点独立维护一个熔断器：失败率达到阈值后进入 OPEN 状态，
 * 不再放行请求；超过熔断时间后进入 HALF_OPEN 状态试探恢复。</p>
 */
public class ResiliencePolicy {

    private final ConcurrentMap<String, EndpointCircuitBreaker> breakers = new ConcurrentHashMap<String, EndpointCircuitBreaker>();

    /**
     * 执行带重试和熔断保护的调用动作。
     */
    public HttpCallResult execute(String key, ApiConfig config, Supplier<HttpCallResult> action) {
        EndpointCircuitBreaker breaker = breakers.computeIfAbsent(key,
                k -> new EndpointCircuitBreaker(
                        config.getCircuitBreakerThreshold(),
                        config.getCircuitBreakerMinCalls(),
                        config.getCircuitBreakerOpenTimeoutMs()));
        // 熔断打开期间直接拒绝请求，避免继续打到不健康的服务
        if (!breaker.isCallPermitted()) {
            HttpCallResult rejected = new HttpCallResult();
            rejected.setErrorType("CIRCUIT_OPEN");
            rejected.setErrorMessage("Circuit breaker is open for " + key);
            return rejected;
        }

        // 总调用次数 = 1 次原始调用 + maxRetries 次重试
        int attempts = Math.max(1, config.getMaxRetries() + 1);
        HttpCallResult result = null;
        for (int i = 0; i < attempts; i++) {
            result = action.get();
            if (result.isSuccess()) {
                breaker.recordSuccess();
                break;
            }
            if (isRetryable(result) && i < attempts - 1) {
                sleep(config.getRetryBackoffMs() * (i + 1));
                continue;
            }
            breaker.recordFailure();
            break;
        }
        return result;
    }

    /**
     * IO 错误、429 限流和 5xx 服务端错误视为可重试。
     */
    private boolean isRetryable(HttpCallResult result) {
        return "IO".equals(result.getErrorType())
                || result.getStatusCode() == 429
                || result.getStatusCode() >= 500;
    }

    /**
     * 重试间隔等待，被中断时恢复中断标记。
     */
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

    /**
     * 单端点熔断器，维护 CLOSED、OPEN、HALF_OPEN 三种状态。
     */
    private static class EndpointCircuitBreaker {

        private enum State {
            CLOSED, OPEN, HALF_OPEN
        }

        private final int failureThreshold;
        private final int minCalls;
        private final long openTimeoutMs;
        private State state = State.CLOSED;
        private int failures;
        private int successes;
        private long openUntil;

        private EndpointCircuitBreaker(int failureThreshold, int minCalls, long openTimeoutMs) {
            this.failureThreshold = failureThreshold;
            this.minCalls = Math.max(1, minCalls);
            this.openTimeoutMs = Math.max(1000L, openTimeoutMs);
        }

        /**
         * 判断当前调用是否被允许；OPEN 到期后进入 HALF_OPEN 放行一个试探请求。
         */
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

        /**
         * 记录一次成功；HALF_OPEN 下成功即关闭熔断，CLOSED 下滚动统计窗口。
         */
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

        /**
         * 记录一次失败；HALF_OPEN 下失败立即重新打开熔断，
         * CLOSED 下统计窗口内失败率达到阈值时打开熔断。
         */
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

        /**
         * 重置统计窗口，重新累计成功与失败次数。
         */
        private void resetWindow() {
            failures = 0;
            successes = 0;
        }
    }
}
