package com.thirdapi.starter.client;

import com.thirdapi.starter.executor.ApiInvocationExecutor;
import com.thirdapi.starter.registry.ThirdApiRegistry;

import java.lang.reflect.Proxy;

/**
 * 为标注了 @ThirdPartyApi 的接口创建 JDK 动态代理。
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
        // 先注册接口中的端点定义，再创建代理，保证调用前定义已经可解析
        registry.register(clientType);
        return (T) Proxy.newProxyInstance(
                clientType.getClassLoader(),
                new Class<?>[]{clientType},
                new ThirdApiInvocationHandler(clientType, registry, executor));
    }
}
