package com.thirdapi.starter.client;

import com.thirdapi.starter.executor.ApiInvocationExecutor;
import com.thirdapi.starter.registry.ThirdApiRegistry;

import java.lang.reflect.Proxy;

/**
 * Creates JDK proxies for annotated third-party API interfaces.
 */
public class ThirdApiClientFactory {

    private final ThirdApiRegistry registry;
    private final ApiInvocationExecutor executor;

    public ThirdApiClientFactory(ThirdApiRegistry registry, ApiInvocationExecutor executor) {
        this.registry = registry;
        this.executor = executor;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> clientType) {
        registry.register(clientType);
        return (T) Proxy.newProxyInstance(
                clientType.getClassLoader(),
                new Class<?>[]{clientType},
                new ThirdApiInvocationHandler(clientType, registry, executor));
    }
}
