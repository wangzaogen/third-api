package com.thirdapi.starter.logging;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryCallMetrics implements CallMetrics {

    private final ConcurrentMap<String, AtomicLong> counters = new ConcurrentHashMap<String, AtomicLong>();
    private final ConcurrentMap<String, AtomicLong> totalCost = new ConcurrentHashMap<String, AtomicLong>();

    @Override
    public void record(ApiCallLog callLog) {
        String key = callLog.getProvider() + "." + callLog.getChannel() + "." + callLog.getEndpoint();
        counters.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        totalCost.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(callLog.getCostMs());
    }

    public long count(String key) {
        AtomicLong value = counters.get(key);
        return value == null ? 0 : value.get();
    }

    public long totalCost(String key) {
        AtomicLong value = totalCost.get(key);
        return value == null ? 0 : value.get();
    }
}
