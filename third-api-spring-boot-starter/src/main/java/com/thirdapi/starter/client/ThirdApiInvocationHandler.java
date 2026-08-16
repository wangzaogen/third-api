package com.thirdapi.starter.client;

import com.thirdapi.sdk.core.model.ApiInvocation;
import com.thirdapi.sdk.core.model.ThirdApiEndpointDefinition;
import com.thirdapi.starter.executor.ApiInvocationExecutor;
import com.thirdapi.starter.registry.ThirdApiRegistry;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ThirdApiInvocationHandler implements InvocationHandler {

    private final Class<?> clientType;
    private final ThirdApiRegistry registry;
    private final ApiInvocationExecutor executor;

    public ThirdApiInvocationHandler(Class<?> clientType,
                                     ThirdApiRegistry registry,
                                     ApiInvocationExecutor executor) {
        this.clientType = clientType;
        this.registry = registry;
        this.executor = executor;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(proxy, method, args);
        }
        ThirdApiEndpointDefinition definition = registry.resolve(clientType, method);
        if (definition == null) {
            throw new IllegalArgumentException("No @ApiMethod definition for " + method);
        }
        ApiInvocation invocation = new ApiInvocation();
        invocation.setProvider(definition.getProvider());
        invocation.setChannel(definition.getChannel());
        invocation.setEndpoint(definition.getEndpoint());
        invocation.setBaseUrl(definition.getBaseUrl());
        invocation.setPath(definition.getPath());
        invocation.setHttpMethod(definition.getHttpMethod());
        invocation.setArgs(args);
        invocation.setMethod(method);
        invocation.setReturnType(method.getReturnType());
        invocation.setStartedAtMillis(System.currentTimeMillis());
        return executor.execute(invocation);
    }

    private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if ("toString".equals(name)) {
            return "ThirdPartyApiClient[" + clientType.getName() + "]";
        }
        if ("hashCode".equals(name)) {
            return System.identityHashCode(this);
        }
        if ("equals".equals(name)) {
            return proxy == args[0];
        }
        throw new UnsupportedOperationException("Unsupported Object method " + name);
    }
}
