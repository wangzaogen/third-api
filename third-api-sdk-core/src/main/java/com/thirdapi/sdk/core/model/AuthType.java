package com.thirdapi.sdk.core.model;

/**
 * 支持的鉴权方式。
 */
public enum AuthType {
    /** 无鉴权。 */
    NONE,
    /** API Key，默认写入 X-Api-Key 请求头。 */
    API_KEY,
    /** HTTP Basic 认证。 */
    BASIC,
    /** OAuth2 Bearer Token。 */
    OAUTH2,
    /** 自定义签名。 */
    SIGN
}
