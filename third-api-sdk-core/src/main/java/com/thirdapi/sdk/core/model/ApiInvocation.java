package com.thirdapi.sdk.core.model;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Runtime context of one third-party call.
 */
public class ApiInvocation {

    private String provider;
    private String channel;
    private String endpoint;
    private String baseUrl;
    private String path;
    private HttpMethod httpMethod;
    private Object[] args;
    private Method method;
    private Class<?> returnType;
    private long startedAtMillis;

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

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public long getStartedAtMillis() {
        return startedAtMillis;
    }

    public void setStartedAtMillis(long startedAtMillis) {
        this.startedAtMillis = startedAtMillis;
    }

    @Override
    public String toString() {
        return "ApiInvocation{"
                + "provider='" + provider + '\''
                + ", channel='" + channel + '\''
                + ", endpoint='" + endpoint + '\''
                + ", path='" + path + '\''
                + ", httpMethod=" + httpMethod
                + ", args=" + Arrays.toString(args)
                + ", method=" + method
                + '}';
    }
}
