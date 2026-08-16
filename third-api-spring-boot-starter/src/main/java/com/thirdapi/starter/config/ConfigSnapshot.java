package com.thirdapi.starter.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次配置快照，包含版本号与全部接口配置。
 */
public class ConfigSnapshot {

    /** 配置版本号，管理端模式下用于增量拉取和 304 判断。 */
    private long version;
    /** 当前快照包含的接口配置列表。 */
    private List<ApiConfig> configs = new ArrayList<ApiConfig>();

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public List<ApiConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(List<ApiConfig> configs) {
        this.configs = configs;
    }
}
