package com.thirdapi.sdk.core.model;

/**
 * 方法参数在 HTTP 请求中的位置。
 */
public enum ParamLocation {
    /** 路径占位符。 */
    PATH,
    /** URL 查询串。 */
    QUERY,
    /** 请求头。 */
    HEADER,
    /** 请求体。 */
    BODY
}
