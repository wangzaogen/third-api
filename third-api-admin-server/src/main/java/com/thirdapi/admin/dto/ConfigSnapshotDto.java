package com.thirdapi.admin.dto;

import java.util.ArrayList;
import java.util.List;

public class ConfigSnapshotDto {

    private long version;
    private List<ApiConfigDto> configs = new ArrayList<ApiConfigDto>();

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public List<ApiConfigDto> getConfigs() {
        return configs;
    }

    public void setConfigs(List<ApiConfigDto> configs) {
        this.configs = configs;
    }
}
