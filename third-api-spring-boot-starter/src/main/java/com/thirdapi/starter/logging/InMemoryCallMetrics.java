package com.thirdapi.starter.logging;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存指标实现，按 provider.channel.endpoint 聚合调用次数与总耗时。
 */
public class InMemoryCallMetrics implements CallMetrics {

    /** 每个端点的累计调用次数。 */
    private final ConcurrentMap<String, AtomicLong> counters = new ConcurrentHashMap<String, AtomicLong>();
    /** 每个端点的累计调用总耗时（毫秒）。 */
    private final ConcurrentMap<String, AtomicLong> totalCost = new ConcurrentHashMap<String, AtomicLong>();

    @Override
    public void record(ApiCallLog callLog) {
        String key = callLog.getProvider() + "." + callLog.getChannel() + "." + callLog.getEndpoint();
        // 先创建计数再累加，保证并发下不丢数据
        counters.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        totalCost.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(callLog.getCostMs());
    }

    /**
     * 查询指定端点的调用次数。
     */
    public long count(String key) {
        AtomicLong value = counters.get(key);
        return value == null ? 0 : value.get();
    }

    /**
     * 查询指定端点的累计调用总耗时。
     */
    public long totalCost(String key) {
        AtomicLong value = totalCost.get(key);
        return value == null ? 0 : value.get();
    }
}
