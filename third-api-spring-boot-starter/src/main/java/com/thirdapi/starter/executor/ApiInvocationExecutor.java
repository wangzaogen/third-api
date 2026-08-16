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
 * 统一调用流水线：请求构建、鉴权、重试/熔断、响应映射、日志与指标。
 *
 * <p>代理客户端的所有业务调用都会进入该执行器，调用结果按接口返回类型
 * 映射为 String、ApiResult 或业务 POJO。</p>
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

    /**
     * 执行一次第三方接口调用，并输出日志与指标。
     */
    public Object execute(ApiInvocation invocation) {
        String key = invocation.getProvider() + "." + invocation.getChannel() + "." + invocation.getEndpoint();
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long startedAt = System.currentTimeMillis();

        ApiConfig config = configStore.get(key);
        if (config == null) {
            // 管理端或本地配置未下发时，使用注解定义与全局默认值兜底
            config = buildFallbackConfig(invocation);
        }
        if (!config.isEnabled()) {
            // 接口被禁用时直接返回失败，不发起真实 HTTP 请求
            ApiCallLog callLog = newCallLog(invocation, traceId, startedAt);
            callLog.setSuccess(false);
            callLog.setErrorType("DISABLED");
            callLog.setErrorMessage("Endpoint is disabled");
            record(callLog);
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

            ApiCallLog callLog = newCallLog(invocation, traceId, startedAt);
            callLog.setHttpMethod(request.getMethod());
            callLog.setUrl(request.getUrl());
            callLog.setRequestBody(truncate(request.getBody()));
            callLog.setResponseBody(truncate(result.getBody()));
            callLog.setHttpStatus(result.getStatusCode());
            callLog.setSuccess(result.isSuccess());
            callLog.setErrorType(result.getErrorType());
            callLog.setErrorMessage(result.getErrorMessage());
            record(callLog);

            return mapResult(invocation, result);
        } catch (RuntimeException e) {
            ApiCallLog callLog = newCallLog(invocation, traceId, startedAt);
            callLog.setSuccess(false);
            callLog.setErrorType(e.getClass().getSimpleName());
            callLog.setErrorMessage(e.getMessage());
            record(callLog);
            return buildFailure(invocation, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 创建带公共字段与耗时的调用日志。
     */
    private ApiCallLog newCallLog(ApiInvocation invocation, String traceId, long startedAt) {
        ApiCallLog callLog = new ApiCallLog();
        fillBase(callLog, invocation, traceId, startedAt);
        callLog.setCostMs(System.currentTimeMillis() - startedAt);
        return callLog;
    }

    /**
     * 输出调用日志并同步记录指标。
     */
    private void record(ApiCallLog callLog) {
        callLogger.log(callLog);
        callMetrics.record(callLog);
    }

    /**
     * 基于注解定义和全局默认配置生成兜底配置。
     */
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

    /**
     * 按接口返回类型映射响应：void 返回 null，String 返回原文，
     * ApiResult 或业务 POJO 通过 JSON 反序列化。
     */
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

    /**
     * 返回类型为 ApiResult 时包装失败结果，其他类型返回 null。
     */
    private Object buildFailure(ApiInvocation invocation, String code, String message) {
        Class<?> returnType = invocation.getReturnType();
        if (returnType != null && returnType == ApiResult.class) {
            return ApiResult.fail(code, message);
        }
        return null;
    }

    /**
     * 填充调用日志中与请求来源相关的公共字段。
     */
    private void fillBase(ApiCallLog callLog, ApiInvocation invocation, String traceId, long startedAt) {
        callLog.setTraceId(traceId);
        callLog.setAppName(properties.getAppName());
        callLog.setProvider(invocation.getProvider());
        callLog.setChannel(invocation.getChannel());
        callLog.setEndpoint(invocation.getEndpoint());
        callLog.setRequestAtMillis(startedAt);
    }

    /**
     * 截断超过 2000 字符的请求/响应报文，避免日志过大。
     */
    private String truncate(String value) {
        if (value == null || value.length() <= 2000) {
            return value;
        }
        return value.substring(0, 2000);
    }
}
