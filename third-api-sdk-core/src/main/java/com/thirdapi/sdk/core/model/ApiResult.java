package com.thirdapi.sdk.core.model;

/**
 * 第三方接口响应的统一返回包装。
 *
 * <p>调用方可通过 isSuccess 判断业务是否成功，code 为业务码，
 * data 存放返回数据，costMs 记录调用耗时。</p>
 */
public class ApiResult<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private long costMs;

    /**
     * 构造成功结果，成功码统一为 0。
     */
    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> result = new ApiResult<T>();
        result.setSuccess(true);
        result.setCode("0");
        result.setData(data);
        return result;
    }

    /**
     * 构造失败结果，业务码与错误信息由调用方传入。
     */
    public static <T> ApiResult<T> fail(String code, String message) {
        ApiResult<T> result = new ApiResult<T>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getCostMs() {
        return costMs;
    }

    public void setCostMs(long costMs) {
        this.costMs = costMs;
    }
}
