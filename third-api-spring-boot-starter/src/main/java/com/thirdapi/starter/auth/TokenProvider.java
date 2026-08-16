package com.thirdapi.starter.auth;

import com.thirdapi.starter.config.ApiConfig;

/**
 * 动态令牌提供者 SPI，用于 OAuth2 等需要获取 access_token 的鉴权方式。
 */
public interface TokenProvider {

    /**
     * 返回当前可用的令牌；实现类负责令牌缓存与过期刷新。
     */
    String getToken(ApiConfig config);
}
