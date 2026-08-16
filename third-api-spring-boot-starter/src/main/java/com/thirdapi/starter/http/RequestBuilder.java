package com.thirdapi.starter.http;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.thirdapi.sdk.core.annotation.ApiParam;
import com.thirdapi.sdk.core.model.ApiInvocation;
import com.thirdapi.sdk.core.model.HttpMethod;
import com.thirdapi.sdk.core.model.ParamLocation;
import com.thirdapi.starter.config.ApiConfig;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 根据接口方法注解和运行时配置构建 HTTP 请求。
 *
 * <p>负责解析参数位置、拼接基础地址与路径、替换路径占位符、
 * 追加查询参数，并序列化请求体。</p>
 */
public class RequestBuilder {

    private final ObjectMapper objectMapper;

    public RequestBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 构建请求：运行时配置优先，其次使用注解定义中的地址、路径与 HTTP 方法。
     */
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

        // 按参数注解将入参分发到路径、查询串、请求头或请求体
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

        // 无注解的单个参数按请求类型处理：GET 类参数放入查询串，POST 类参数作为请求体
        if (unannotatedCount == 1 && bodyObject == null && !isBodyAllowed(httpMethod)) {
            if (unannotated instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) unannotated).entrySet()) {
                    queryParams.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            } else {
                queryParams.put("arg", String.valueOf(unannotated));
            }
        } else if (unannotatedCount == 1 && bodyObject == null && isBodyAllowed(httpMethod)) {
            bodyObject = unannotated;
        }

        if (bodyObject != null && isBodyAllowed(httpMethod)) {
            request.setBody(serializeBody(bodyObject));
        }

        request.setUrl(appendQuery(url, queryParams));
        return request;
    }

    /**
     * 字符串参数直接作为请求体，其他对象使用 ObjectMapper 序列化为 JSON。
     */
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

    /**
     * 拼接基础地址与请求路径，去掉重复的斜杠。
     */
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

    /**
     * 将 {参数名} 占位符替换为 URL 编码后的路径参数。
     */
    private String applyPathParams(String url, Map<String, String> pathParams) {
        String result = url;
        for (Map.Entry<String, String> entry : pathParams.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", encode(entry.getValue()));
        }
        return result;
    }

    /**
     * 将查询参数按 key=value 追加到 URL，参数值做 URL 编码。
     */
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

    /**
     * 判断该 HTTP 方法是否允许携带请求体。
     */
    private boolean isBodyAllowed(String method) {
        return "POST".equals(method)
                || "PUT".equals(method)
                || "PATCH".equals(method)
                || "DELETE".equals(method);
    }

    /**
     * 对路径或查询参数做 UTF-8 URL 编码。
     */
    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported", e);
        }
    }
}
