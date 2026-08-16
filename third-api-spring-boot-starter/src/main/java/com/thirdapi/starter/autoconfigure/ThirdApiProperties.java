package com.thirdapi.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * third-api.* configuration properties.
 */
@ConfigurationProperties(prefix = "third-api")
public class ThirdApiProperties {

    private boolean enabled = true;
    private String mode = "local";
    private String appName = "unknown";
    private String adminUrl = "";
    private String appId = "";
    private String appSecret = "";
    private int cacheTtlSeconds = 60;
    private int pollIntervalSeconds = 30;
    private int longPollTimeoutSeconds = 30;
    private Timeout defaultTimeout = new Timeout();
    private Retry defaultRetry = new Retry();
    private CircuitBreaker defaultCircuitBreaker = new CircuitBreaker();
    private List<Endpoint> endpoints = new ArrayList<Endpoint>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public int getLongPollTimeoutSeconds() {
        return longPollTimeoutSeconds;
    }

    public void setLongPollTimeoutSeconds(int longPollTimeoutSeconds) {
        this.longPollTimeoutSeconds = longPollTimeoutSeconds;
    }

    public Timeout getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(Timeout defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public Retry getDefaultRetry() {
        return defaultRetry;
    }

    public void setDefaultRetry(Retry defaultRetry) {
        this.defaultRetry = defaultRetry;
    }

    public CircuitBreaker getDefaultCircuitBreaker() {
        return defaultCircuitBreaker;
    }

    public void setDefaultCircuitBreaker(CircuitBreaker defaultCircuitBreaker) {
        this.defaultCircuitBreaker = defaultCircuitBreaker;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public static class Timeout {

        private int connectMs = 3000;
        private int readMs = 5000;

        public int getConnectMs() {
            return connectMs;
        }

        public void setConnectMs(int connectMs) {
            this.connectMs = connectMs;
        }

        public int getReadMs() {
            return readMs;
        }

        public void setReadMs(int readMs) {
            this.readMs = readMs;
        }
    }

    public static class Retry {

        private int maxAttempts = 2;
        private long backoffMs = 200L;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getBackoffMs() {
            return backoffMs;
        }

        public void setBackoffMs(long backoffMs) {
            this.backoffMs = backoffMs;
        }
    }

    public static class CircuitBreaker {

        private int failureRatioThreshold = 50;
        private int minCalls = 5;
        private long openTimeoutMs = 10000L;

        public int getFailureRatioThreshold() {
            return failureRatioThreshold;
        }

        public void setFailureRatioThreshold(int failureRatioThreshold) {
            this.failureRatioThreshold = failureRatioThreshold;
        }

        public int getMinCalls() {
            return minCalls;
        }

        public void setMinCalls(int minCalls) {
            this.minCalls = minCalls;
        }

        public long getOpenTimeoutMs() {
            return openTimeoutMs;
        }

        public void setOpenTimeoutMs(long openTimeoutMs) {
            this.openTimeoutMs = openTimeoutMs;
        }
    }

    public static class Endpoint {

        private String provider;
        private String channel;
        private String name;
        private String baseUrl;
        private String path;
        private String method = "POST";
        private int timeoutMs = -1;
        private int maxRetries = -1;
        private long retryBackoffMs = -1;
        private String authType = "NONE";
        private String tokenUrl;
        private String clientId;
        private String clientSecret;
        private String apiKey;
        private Map<String, String> extraAuthConfig;
        private boolean enabled = true;

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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
