package com.thirdapi.starter.config;

import com.thirdapi.starter.autoconfigure.ThirdApiProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the initial snapshot from third-api.endpoints.* properties.
 */
public class LocalConfigSource implements ConfigSource {

    private final ThirdApiProperties properties;

    public LocalConfigSource(ThirdApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "local";
    }

    @Override
    public ConfigSnapshot load() {
        ConfigSnapshot snapshot = new ConfigSnapshot();
        snapshot.setVersion(0L);
        List<ApiConfig> configs = new ArrayList<ApiConfig>();
        for (ThirdApiProperties.Endpoint source : properties.getEndpoints()) {
            configs.add(toConfig(source));
        }
        snapshot.setConfigs(configs);
        return snapshot;
    }

    private ApiConfig toConfig(ThirdApiProperties.Endpoint source) {
        ApiConfig config = new ApiConfig();
        config.setProvider(source.getProvider());
        config.setChannel(source.getChannel());
        config.setEndpoint(source.getName());
        config.setBaseUrl(source.getBaseUrl());
        config.setPath(source.getPath());
        config.setHttpMethod(source.getMethod() == null ? "POST" : source.getMethod().toUpperCase());
        config.setEnabled(source.isEnabled());
        config.setTimeoutMs(source.getTimeoutMs() > 0
                ? source.getTimeoutMs()
                : properties.getDefaultTimeout().getReadMs());
        config.setMaxRetries(source.getMaxRetries() >= 0
                ? source.getMaxRetries()
                : properties.getDefaultRetry().getMaxAttempts());
        config.setRetryBackoffMs(source.getRetryBackoffMs() >= 0
                ? source.getRetryBackoffMs()
                : properties.getDefaultRetry().getBackoffMs());
        config.setAuthType(source.getAuthType() == null ? "NONE" : source.getAuthType().toUpperCase());
        config.setTokenUrl(source.getTokenUrl());
        config.setClientId(source.getClientId());
        config.setClientSecret(source.getClientSecret());
        config.setApiKey(source.getApiKey());
        config.setExtraAuthConfig(source.getExtraAuthConfig());
        config.setCircuitBreakerThreshold(properties.getDefaultCircuitBreaker().getFailureRatioThreshold());
        config.setCircuitBreakerMinCalls(properties.getDefaultCircuitBreaker().getMinCalls());
        config.setCircuitBreakerOpenTimeoutMs(properties.getDefaultCircuitBreaker().getOpenTimeoutMs());
        return config;
    }
}
