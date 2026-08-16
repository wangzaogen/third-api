package com.thirdapi.starter.registry;

import com.thirdapi.sdk.core.annotation.ApiMethod;
import com.thirdapi.sdk.core.annotation.ThirdPartyApi;
import com.thirdapi.sdk.core.model.ThirdApiEndpointDefinition;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 本地端点注册表，从标注了注解的客户端接口中解析并缓存端点定义。
 */
public class ThirdApiRegistry {

    /** 端点定义缓存，key 为 provider.channel.endpoint。 */
    private final ConcurrentMap<String, ThirdApiEndpointDefinition> endpoints = new ConcurrentHashMap<String, ThirdApiEndpointDefinition>();

    /**
     * 解析接口上的 @ThirdPartyApi 与 @ApiMethod，并将方法注册为端点定义。
     */
    public void register(Class<?> clientType) {
        ThirdPartyApi api = clientType.getAnnotation(ThirdPartyApi.class);
        if (api == null) {
            return;
        }
        for (Method method : clientType.getMethods()) {
            ApiMethod apiMethod = method.getAnnotation(ApiMethod.class);
            if (apiMethod == null) {
                continue;
            }
            // 未显式指定端点编码时使用方法名作为稳定标识
            String endpoint = apiMethod.name().isEmpty() ? method.getName() : apiMethod.name();
            ThirdApiEndpointDefinition definition = new ThirdApiEndpointDefinition(
                    api.provider(),
                    api.channel(),
                    endpoint,
                    api.baseUrl(),
                    apiMethod.path(),
                    apiMethod.method(),
                    apiMethod.timeoutMs(),
                    apiMethod.maxRetries());
            endpoints.put(definition.key(), definition);
        }
    }

    /**
     * 根据客户端接口和方法解析对应的端点定义。
     */
    public ThirdApiEndpointDefinition resolve(Class<?> clientType, Method method) {
        ThirdPartyApi api = clientType.getAnnotation(ThirdPartyApi.class);
        ApiMethod apiMethod = method.getAnnotation(ApiMethod.class);
        if (api == null || apiMethod == null) {
            return null;
        }
        String endpoint = apiMethod.name().isEmpty() ? method.getName() : apiMethod.name();
        String key = api.provider() + "." + api.channel() + "." + endpoint;
        return endpoints.get(key);
    }

    /**
     * 返回全部已注册的端点定义。
     */
    public Collection<ThirdApiEndpointDefinition> all() {
        return endpoints.values();
    }
}
