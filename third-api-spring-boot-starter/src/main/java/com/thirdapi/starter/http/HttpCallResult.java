package com.thirdapi.starter.http;

/**
 * 一次 HTTP 调用的结果，包含状态码、响应体与错误信息。
 */
public class HttpCallResult {

    private int statusCode;
    private String body;
    private String errorType;
    private String errorMessage;

    public HttpCallResult() {
    }

    public HttpCallResult(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    /**
     * 2xx 状态码视为调用成功。
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
