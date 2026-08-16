package com.thirdapi.starter.registry;

import com.thirdapi.sdk.core.annotation.ApiMethod;
import com.thirdapi.sdk.core.annotation.ThirdPartyApi;
import com.thirdapi.sdk.core.model.ThirdApiEndpointDefinition;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Local registry of endpoint definitions discovered from annotated client interfaces.
 */
public class ThirdApiRegistry {

    private final ConcurrentMap<String, ThirdApiEndpointDefinition> endpoints = new ConcurrentHashMap<String, ThirdApiEndpointDefinition>();

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

    public Collection<ThirdApiEndpointDefinition> all() {
        return endpoints.values();
    }
}
