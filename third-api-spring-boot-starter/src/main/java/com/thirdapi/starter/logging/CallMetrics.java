package com.thirdapi.starter.logging;

/**
 * 调用指标 SPI，负责记录接口调用的次数与耗时等指标。
 */
public interface CallMetrics {

    /**
     * 记录一次调用对应的指标。
     */
    void record(ApiCallLog callLog);
}
