package com.thirdapi.starter.logging;

/**
 * 调用日志 SPI，负责输出每一次第三方接口调用的日志。
 */
public interface CallLogger {

    /**
     * 输出一条调用日志。
     */
    void log(ApiCallLog callLog);
}
