package com.thirdapi.starter.config;

/**
 * 配置源 SPI，负责加载接口配置快照。
 *
 * <p>当前内置本地配置源（third-api.endpoints.*）与管理端配置源，
 * 业务系统也可以自行实现该接口接入其他配置中心。</p>
 */
public interface ConfigSource {

    /**
     * 配置源名称，用于日志和告警中区分来源。
     */
    String name();

    /**
     * 返回新的配置快照；配置没有变化时返回 null。
     */
    ConfigSnapshot load();
}
