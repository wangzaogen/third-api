package com.thirdapi.sdk.core.model;

/**
 * 注册后的第三方接口端点不可变定义。
 *
 * <p>保存从注解中解析出的静态契约；运行时动态配置（如管理端下发）会
 * 覆盖其中可变的地址、超时和重试等参数。</p>
 */
public class ThirdApiEndpointDefinition {

    private final String provider;
    private final String channel;
    private final String endpoint;
    private final String baseUrl;
    private final String path;
    private final HttpMethod httpMethod;
    private final int timeoutMs;
    private final int maxRetries;

    public ThirdApiEndpointDefinition(String provider,
                                      String channel,
                                      String endpoint,
                                      String baseUrl,
                                      String path,
                                      HttpMethod httpMethod,
                                      int timeoutMs,
                                      int maxRetries) {
        this.provider = provider;
        this.channel = channel;
        this.endpoint = endpoint;
        this.baseUrl = baseUrl;
        this.path = path;
        this.httpMethod = httpMethod;
        this.timeoutMs = timeoutMs;
        this.maxRetries = maxRetries;
    }

    public String getProvider() {
        return provider;
    }

    public String getChannel() {
        return channel;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getPath() {
        return path;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 返回 provider.channel.endpoint 组成的全局唯一标识，用于关联运行时配置。
     */
    public String key() {
        return provider + "." + channel + "." + endpoint;
    }
}
