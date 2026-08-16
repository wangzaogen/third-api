package com.thirdapi.starter.http;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.thirdapi.sdk.core.annotation.ApiParam;
import com.thirdapi.sdk.core.model.ApiInvocation;
import com.thirdapi.sdk.core.model.HttpMethod;
import com.thirdapi.sdk.core.model.ParamLocation;
import com.thirdapi.sdk.core.model.ThirdApiEndpointDefinition;
import com.thirdapi.starter.config.ApiConfig;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds an HTTP request from an annotated client method and runtime config.
 */
public class RequestBuilder {

    private final ObjectMapper objectMapper;

    public RequestBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ApiRequest build(ApiInvocation invocation, ApiConfig config) {
        ApiRequest request = new ApiRequest();
        String baseUrl = config != null && config.getBaseUrl() != null
                ? config.getBaseUrl()
                : invocation.getBaseUrl();
        String path = config != null && config.getPath() != null
                ? config.getPath()
                : invocation.getPath();
        String httpMethod = config != null && config.getHttpMethod() != null
                ? config.getHttpMethod()
                : invocation.getHttpMethod().name();
        request.setMethod(httpMethod.toUpperCase());
        request.addHeader("Accept", "application/json");

        Map<String, String> pathParams = new LinkedHashMap<String, String>();
        Map<String, String> queryParams = new LinkedHashMap<String, String>();
        Object bodyObject = null;

        Method method = invocation.getMethod();
        Object[] args = invocation.getArgs() == null ? new Object[0] : invocation.getArgs();
        Parameter[] parameters = method.getParameters();
        int unannotatedCount = 0;
        Object unannotated = null;

        for (int i = 0; i < parameters.length; i++) {
            ApiParam param = parameters[i].getAnnotation(ApiParam.class);
            if (param == null) {
                unannotatedCount++;
                unannotated = i < args.length ? args[i] : null;
                continue;
            }
            Object value = i < args.length ? args[i] : null;
            String name = param.name().isEmpty() ? "arg" + i : param.name();
            if (value == null) {
                if (param.required()) {
                    throw new IllegalArgumentException("Missing required param " + name);
                }
                continue;
            }
            if (param.location() == ParamLocation.PATH) {
                pathParams.put(name, String.valueOf(value));
            } else if (param.location() == ParamLocation.QUERY) {
                queryParams.put(name, String.valueOf(value));
            } else if (param.location() == ParamLocation.HEADER) {
                request.addHeader(name, String.valueOf(value));
            } else {
                bodyObject = value;
            }
        }

        String url = joinUrl(baseUrl, path);
        url = applyPathParams(url, pathParams);
        url = appendQuery(url, queryParams);

        if (unannotatedCount == 1 && bodyObject == null && !isBodyAllowed(httpMethod)) {
            if (unannotated instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) unannotated).entrySet()) {
                    queryParams.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            } else {
                queryParams.put("arg", String.valueOf(unannotated));
            }
            url = appendQuery(url, queryParams);
        } else if (unannotatedCount == 1 && bodyObject == null && isBodyAllowed(httpMethod)) {
            bodyObject = unannotated;
        }

        if (bodyObject != null && isBodyAllowed(httpMethod)) {
            request.setBody(serializeBody(bodyObject));
        }

        request.setUrl(url);
        return request;
    }

    public ApiConfig fallbackConfig(ThirdApiEndpointDefinition definition, ApiConfig defaultConfig) {
        ApiConfig config = new ApiConfig();
        config.setProvider(definition.getProvider());
        config.setChannel(definition.getChannel());
        config.setEndpoint(definition.getEndpoint());
        config.setPath(definition.getPath());
        config.setHttpMethod(definition.getHttpMethod().name());
        config.setBaseUrl(definition.getBaseUrl());
        if (defaultConfig != null) {
            config.setTimeoutMs(defaultConfig.getTimeoutMs());
            config.setMaxRetries(defaultConfig.getMaxRetries());
            config.setRetryBackoffMs(defaultConfig.getRetryBackoffMs());
            config.setAuthType(defaultConfig.getAuthType());
            config.setTokenUrl(defaultConfig.getTokenUrl());
            config.setClientId(defaultConfig.getClientId());
            config.setClientSecret(defaultConfig.getClientSecret());
            config.setApiKey(defaultConfig.getApiKey());
            config.setCircuitBreakerThreshold(defaultConfig.getCircuitBreakerThreshold());
            config.setCircuitBreakerMinCalls(defaultConfig.getCircuitBreakerMinCalls());
            config.setCircuitBreakerOpenTimeoutMs(defaultConfig.getCircuitBreakerOpenTimeoutMs());
        }
        return config;
    }

    private String serializeBody(Object bodyObject) {
        if (bodyObject instanceof String) {
            return (String) bodyObject;
        }
        try {
            return objectMapper.writeValueAsString(bodyObject);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize request body", e);
        }
    }

    private String joinUrl(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        String p = path == null ? "" : path.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!p.isEmpty() && !p.startsWith("/")) {
            p = "/" + p;
        }
        return base + p;
    }

    private String applyPathParams(String url, Map<String, String> pathParams) {
        String result = url;
        for (Map.Entry<String, String> entry : pathParams.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", encode(entry.getValue()));
        }
        return result;
    }

    private String appendQuery(String url, Map<String, String> queryParams) {
        if (queryParams.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        sb.append(url.contains("?") ? "&" : "?");
        int index = 0;
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (index++ > 0) {
                sb.append('&');
            }
            sb.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return sb.toString();
    }

    private boolean isBodyAllowed(String method) {
        return "POST".equals(method)
                || "PUT".equals(method)
                || "PATCH".equals(method)
                || "DELETE".equals(method);
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported", e);
        }
    }
}
