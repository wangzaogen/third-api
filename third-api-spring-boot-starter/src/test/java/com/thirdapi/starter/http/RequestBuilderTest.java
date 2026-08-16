package com.thirdapi.starter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdapi.sdk.core.annotation.ApiMethod;
import com.thirdapi.sdk.core.annotation.ApiParam;
import com.thirdapi.sdk.core.annotation.ThirdPartyApi;
import com.thirdapi.sdk.core.model.ApiInvocation;
import com.thirdapi.sdk.core.model.HttpMethod;
import com.thirdapi.sdk.core.model.ParamLocation;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RequestBuilderTest {

    private final RequestBuilder builder = new RequestBuilder(new ObjectMapper());

    @Test
    public void appendsAnnotatedAndUnannotatedQueryParams() throws Exception {
        Method method = Client.class.getMethod("query", String.class, Map.class);
        Map<String, String> extra = new LinkedHashMap<String, String>();
        extra.put("page", "2");

        ApiRequest request = builder.build(invocation(method, "/search", HttpMethod.GET, "hello world", extra), null);

        assertEquals("http://example.com/search?q=hello+world&page=2", request.getUrl());
    }

    @Test
    public void replacesPathParamsAndEncodesValues() throws Exception {
        Method method = Client.class.getMethod("getById", String.class, boolean.class);

        ApiRequest request = builder.build(invocation(method, "/users/{id}", HttpMethod.GET, "a/b", true), null);

        assertEquals("http://example.com/users/a%2Fb?active=true", request.getUrl());
    }

    @Test
    public void serializesUnannotatedBodyForPost() throws Exception {
        Method method = Client.class.getMethod("create", Map.class);
        Map<String, String> body = new LinkedHashMap<String, String>();
        body.put("name", "demo");

        ApiRequest request = builder.build(invocation(method, "/users", HttpMethod.POST, body), null);

        assertEquals("http://example.com/users", request.getUrl());
        assertEquals("{\"name\":\"demo\"}", request.getBody());
    }

    private ApiInvocation invocation(Method method, String path, HttpMethod httpMethod, Object... args) {
        ApiInvocation invocation = new ApiInvocation();
        invocation.setProvider("demo");
        invocation.setChannel("local");
        invocation.setEndpoint(method.getName());
        invocation.setBaseUrl("http://example.com");
        invocation.setPath(path);
        invocation.setHttpMethod(httpMethod);
        invocation.setArgs(args);
        invocation.setMethod(method);
        invocation.setReturnType(method.getReturnType());
        return invocation;
    }

    @ThirdPartyApi(provider = "demo", channel = "local")
    private interface Client {

        @ApiMethod(name = "query", path = "/search", method = HttpMethod.GET)
        String query(@ApiParam(name = "q", location = ParamLocation.QUERY) String q, Map<String, String> extra);

        @ApiMethod(name = "getById", path = "/users/{id}", method = HttpMethod.GET)
        String getById(@ApiParam(name = "id", location = ParamLocation.PATH) String id,
                       @ApiParam(name = "active", location = ParamLocation.QUERY) boolean active);

        @ApiMethod(name = "create", path = "/users", method = HttpMethod.POST)
        String create(Map<String, String> body);
    }
}
