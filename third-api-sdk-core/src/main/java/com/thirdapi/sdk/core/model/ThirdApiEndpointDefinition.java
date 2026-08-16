package com.thirdapi.sdk.core.model;

/**
 * Immutable snapshot of a registered third-party endpoint.
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

    public String key() {
        return provider + "." + channel + "." + endpoint;
    }
}
