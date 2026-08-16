package com.thirdapi.starter.config;

import java.util.Map;

/**
 * 单个第三方接口的运行时配置快照。
 *
 * <p>配置可来自本地属性文件或管理端动态下发，执行器在每次调用时
 * 从 ConfigStore 读取并生效，从而支持配置热更新。</p>
 */
public class ApiConfig {

    private String provider;
    private String channel;
    private String endpoint;
    private String baseUrl;
    private String path;
    private String httpMethod = "POST";
    private boolean enabled = true;
    private int timeoutMs = 5000;
    private int maxRetries = 2;
    private long retryBackoffMs = 200L;
    private String authType = "NONE";
    private String tokenUrl;
    private String clientId;
    private String clientSecret;
    private String apiKey;
    private Map<String, String> extraAuthConfig;
    private int circuitBreakerThreshold = 50;
    private int circuitBreakerMinCalls = 5;
    private long circuitBreakerOpenTimeoutMs = 10000L;

    /**
     * 返回 provider.channel.endpoint 组成的唯一键，用于配置关联与统计。
     */
    public String key() {
        return provider + "." + channel + "." + endpoint;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(long retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Map<String, String> getExtraAuthConfig() {
        return extraAuthConfig;
    }

    public void setExtraAuthConfig(Map<String, String> extraAuthConfig) {
        this.extraAuthConfig = extraAuthConfig;
    }

    public int getCircuitBreakerThreshold() {
        return circuitBreakerThreshold;
    }

    public void setCircuitBreakerThreshold(int circuitBreakerThreshold) {
        this.circuitBreakerThreshold = circuitBreakerThreshold;
    }

    public int getCircuitBreakerMinCalls() {
        return circuitBreakerMinCalls;
    }

    public void setCircuitBreakerMinCalls(int circuitBreakerMinCalls) {
        this.circuitBreakerMinCalls = circuitBreakerMinCalls;
    }

    public long getCircuitBreakerOpenTimeoutMs() {
        return circuitBreakerOpenTimeoutMs;
    }

    public void setCircuitBreakerOpenTimeoutMs(long circuitBreakerOpenTimeoutMs) {
        this.circuitBreakerOpenTimeoutMs = circuitBreakerOpenTimeoutMs;
    }
}
