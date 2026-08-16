package com.thirdapi.starter.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdapi.sdk.core.annotation.ApiMethod;
import com.thirdapi.sdk.core.model.ApiInvocation;
import com.thirdapi.sdk.core.model.ApiResult;
import com.thirdapi.starter.auth.AuthProcessor;
import com.thirdapi.starter.autoconfigure.ThirdApiProperties;
import com.thirdapi.starter.config.ApiConfig;
import com.thirdapi.starter.config.ConfigStore;
import com.thirdapi.starter.http.ApiRequest;
import com.thirdapi.starter.http.HttpCallResult;
import com.thirdapi.starter.http.RequestBuilder;
import com.thirdapi.starter.http.SimpleHttpClient;
import com.thirdapi.starter.logging.ApiCallLog;
import com.thirdapi.starter.logging.CallLogger;
import com.thirdapi.starter.logging.CallMetrics;
import com.thirdapi.starter.resilience.ResiliencePolicy;

import java.util.UUID;

/**
 * Unified invocation pipeline: request building, auth, retry/circuit breaker,
 * response mapping, logging and metrics.
 */
public class ApiInvocationExecutor {

    private final ConfigStore configStore;
    private final RequestBuilder requestBuilder;
    private final AuthProcessor authProcessor;
    private final SimpleHttpClient httpClient;
    private final ResiliencePolicy resiliencePolicy;
    private final CallLogger callLogger;
    private final CallMetrics callMetrics;
    private final ThirdApiProperties properties;
    private final ObjectMapper objectMapper;

    public ApiInvocationExecutor(ConfigStore configStore,
                                 RequestBuilder requestBuilder,
                                 AuthProcessor authProcessor,
                                 SimpleHttpClient httpClient,
                                 ResiliencePolicy resiliencePolicy,
                                 CallLogger callLogger,
                                 CallMetrics callMetrics,
                                 ThirdApiProperties properties,
                                 ObjectMapper objectMapper) {
        this.configStore = configStore;
        this.requestBuilder = requestBuilder;
        this.authProcessor = authProcessor;
        this.httpClient = httpClient;
        this.resiliencePolicy = resiliencePolicy;
        this.callLogger = callLogger;
        this.callMetrics = callMetrics;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Object execute(ApiInvocation invocation) {
        String key = invocation.getProvider() + "." + invocation.getChannel() + "." + invocation.getEndpoint();
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long startedAt = System.currentTimeMillis();

        ApiConfig config = configStore.get(key);
        if (config == null) {
            config = buildFallbackConfig(invocation);
        }
        if (!config.isEnabled()) {
            ApiCallLog callLog = new ApiCallLog();
            fillBase(callLog, invocation, traceId, startedAt);
            callLog.setSuccess(false);
            callLog.setErrorType("DISABLED");
            callLog.setErrorMessage("Endpoint is disabled");
            callLog.setCostMs(System.currentTimeMillis() - startedAt);
            callLogger.log(callLog);
            callMetrics.record(callLog);
            return buildFailure(invocation, "DISABLED", "Endpoint is disabled");
        }

        try {
            ApiRequest request = requestBuilder.build(invocation, config);
            authProcessor.apply(request, config);
            final ApiRequest finalRequest = request;
            final ApiConfig effectiveConfig = config;
            HttpCallResult result = resiliencePolicy.execute(key, effectiveConfig,
                    () -> httpClient.execute(finalRequest,
                            effectiveConfig.getTimeoutMs(),
                            effectiveConfig.getTimeoutMs()));

            ApiCallLog callLog = new ApiCallLog();
            fillBase(callLog, invocation, traceId, startedAt);
            callLog.setHttpMethod(request.getMethod());
            callLog.setUrl(request.getUrl());
            callLog.setRequestBody(truncate(request.getBody()));
            callLog.setResponseBody(truncate(result.getBody()));
            callLog.setHttpStatus(result.getStatusCode());
            callLog.setSuccess(result.isSuccess());
            callLog.setErrorType(result.getErrorType());
            callLog.setErrorMessage(result.getErrorMessage());
            callLog.setCostMs(System.currentTimeMillis() - startedAt);
            callLogger.log(callLog);
            callMetrics.record(callLog);

            return mapResult(invocation, result);
        } catch (RuntimeException e) {
            ApiCallLog callLog = new ApiCallLog();
            fillBase(callLog, invocation, traceId, startedAt);
            callLog.setSuccess(false);
            callLog.setErrorType(e.getClass().getSimpleName());
            callLog.setErrorMessage(e.getMessage());
            callLog.setCostMs(System.currentTimeMillis() - startedAt);
            callLogger.log(callLog);
            callMetrics.record(callLog);
            return buildFailure(invocation, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private ApiConfig buildFallbackConfig(ApiInvocation invocation) {
        ApiConfig config = new ApiConfig();
        config.setProvider(invocation.getProvider());
        config.setChannel(invocation.getChannel());
        config.setEndpoint(invocation.getEndpoint());
        config.setBaseUrl(invocation.getBaseUrl());
        config.setPath(invocation.getPath());
        config.setHttpMethod(invocation.getHttpMethod() == null ? "POST" : invocation.getHttpMethod().name());
        config.setTimeoutMs(properties.getDefaultTimeout().getReadMs());
        config.setMaxRetries(properties.getDefaultRetry().getMaxAttempts());
        config.setRetryBackoffMs(properties.getDefaultRetry().getBackoffMs());
        config.setCircuitBreakerThreshold(properties.getDefaultCircuitBreaker().getFailureRatioThreshold());
        config.setCircuitBreakerMinCalls(properties.getDefaultCircuitBreaker().getMinCalls());
        config.setCircuitBreakerOpenTimeoutMs(properties.getDefaultCircuitBreaker().getOpenTimeoutMs());
        if (invocation.getMethod() != null) {
            ApiMethod apiMethod = invocation.getMethod().getAnnotation(ApiMethod.class);
            if (apiMethod != null) {
                if (apiMethod.timeoutMs() > 0) {
                    config.setTimeoutMs(apiMethod.timeoutMs());
                }
                if (apiMethod.maxRetries() >= 0) {
                    config.setMaxRetries(apiMethod.maxRetries());
                }
            }
        }
        return config;
    }

    private Object mapResult(ApiInvocation invocation, HttpCallResult result) {
        Class<?> returnType = invocation.getReturnType();
        if (returnType == null || returnType == Void.TYPE) {
            return null;
        }
        if (!result.isSuccess()) {
            return buildFailure(invocation, result.getErrorType() == null ? "HTTP_" + result.getStatusCode() : result.getErrorType(),
                    result.getErrorMessage());
        }
        if (returnType == String.class) {
            return result.getBody();
        }
        if (returnType == ApiResult.class) {
            try {
                return objectMapper.readValue(result.getBody(), ApiResult.class);
            } catch (Exception e) {
                return ApiResult.ok(result.getBody());
            }
        }
        if (result.getBody() == null || result.getBody().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(result.getBody(), returnType);
        } catch (Exception e) {
            return buildFailure(invocation, "PARSE", e.getMessage());
        }
    }

    private Object buildFailure(ApiInvocation invocation, String code, String message) {
        Class<?> returnType = invocation.getReturnType();
        if (returnType != null && returnType == ApiResult.class) {
            return ApiResult.fail(code, message);
        }
        return null;
    }

    private void fillBase(ApiCallLog callLog, ApiInvocation invocation, String traceId, long startedAt) {
        callLog.setTraceId(traceId);
        callLog.setAppName(properties.getAppName());
        callLog.setProvider(invocation.getProvider());
        callLog.setChannel(invocation.getChannel());
        callLog.setEndpoint(invocation.getEndpoint());
        callLog.setRequestAtMillis(startedAt);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 2000) {
            return value;
        }
        return value.substring(0, 2000);
    }
}
