package com.thirdapi.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * third-api.* 配置属性。
 *
 * <p>用于控制启用开关、运行模式、管理端连接、默认超时/重试/熔断参数，
 * 以及本地端点配置列表。</p>
 */
@ConfigurationProperties(prefix = "third-api")
public class ThirdApiProperties {

    /** 总开关，false 时自动配置不生效。 */
    private boolean enabled = true;
    /** 运行模式：local 使用本地配置，admin 从管理端拉取配置。 */
    private String mode = "local";
    /** 应用名称，用于调用日志与指标标识。 */
    private String appName = "unknown";
    /** 管理端基础地址。 */
    private String adminUrl = "";
    /** 管理端应用 ID。 */
    private String appId = "";
    /** 管理端应用密钥。 */
    private String appSecret = "";
    /** 配置缓存时间（秒），供需要缓存的管理端模式使用。 */
    private int cacheTtlSeconds = 60;
    /** admin 模式下配置轮询间隔（秒）。 */
    private int pollIntervalSeconds = 30;
    /** 管理端配置长轮询超时（秒）。 */
    private int longPollTimeoutSeconds = 30;
    /** 默认连接与读取超时配置。 */
    private Timeout defaultTimeout = new Timeout();
    /** 默认重试配置。 */
    private Retry defaultRetry = new Retry();
    /** 默认熔断配置。 */
    private CircuitBreaker defaultCircuitBreaker = new CircuitBreaker();
    /** 本地端点配置列表，对应 third-api.endpoints.*。 */
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

    /**
     * 默认超时配置。
     */
    public static class Timeout {

        /** 连接超时（毫秒）。 */
        private int connectMs = 3000;
        /** 读取超时（毫秒）。 */
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

    /**
     * 默认重试配置。
     */
    public static class Retry {

        /** 最大尝试次数，包含首次调用。 */
        private int maxAttempts = 2;
        /** 重试等待时间基数（毫秒），后续重试按倍数递增。 */
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

    /**
     * 默认熔断配置。
     */
    public static class CircuitBreaker {

        /** 打开熔断的失败率阈值（百分比）。 */
        private int failureRatioThreshold = 50;
        /** 最少统计调用数，达到该数量后才判断失败率。 */
        private int minCalls = 5;
        /** 熔断打开时长（毫秒）。 */
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

    /**
     * 单个本地端点的配置。
     */
    public static class Endpoint {

        /** 服务商编码。 */
        private String provider;
        /** 渠道编码。 */
        private String channel;
        /** 端点编码，需与 @ApiMethod.name 或方法名一致。 */
        private String name;
        /** 基础地址，留空时使用注解上的 baseUrl。 */
        private String baseUrl;
        /** 请求路径。 */
        private String path;
        /** HTTP 方法，默认 POST。 */
        private String method = "POST";
        /** 超时时间（毫秒），小于 0 时使用全局默认值。 */
        private int timeoutMs = -1;
        /** 最大重试次数，小于 0 时使用全局默认值。 */
        private int maxRetries = -1;
        /** 重试等待时间基数（毫秒），小于 0 时使用全局默认值。 */
        private long retryBackoffMs = -1;
        /** 鉴权方式，默认 NONE。 */
        private String authType = "NONE";
        /** OAuth2 获取令牌地址。 */
        private String tokenUrl;
        /** 客户端 ID，用于 Basic 或 OAuth2 鉴权。 */
        private String clientId;
        /** 客户端密钥，用于 Basic、OAuth2 或签名鉴权。 */
        private String clientSecret;
        /** API Key。 */
        private String apiKey;
        /** 鉴权扩展配置，例如自定义 API Key 请求头名。 */
        private Map<String, String> extraAuthConfig;
        /** 是否启用该端点，false 时调用直接返回失败。 */
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
