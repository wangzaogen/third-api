package com.thirdapi.starter.http;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApiRequest {

    private String url;
    private String method = "GET";
    private final Map<String, String> headers = new LinkedHashMap<String, String>();
    private String body;
    private String contentType = "application/json; charset=utf-8";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
