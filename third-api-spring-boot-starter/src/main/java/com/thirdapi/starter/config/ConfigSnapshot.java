package com.thirdapi.starter.config;

import java.util.ArrayList;
import java.util.List;

public class ConfigSnapshot {

    private long version;
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
