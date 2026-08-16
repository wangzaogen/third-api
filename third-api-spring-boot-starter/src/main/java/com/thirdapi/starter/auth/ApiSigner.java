package com.thirdapi.starter.auth;

import com.thirdapi.starter.config.ApiConfig;
import com.thirdapi.starter.http.ApiRequest;

/**
 * 请求签名器 SPI，用于 SIGN 鉴权方式。
 */
public interface ApiSigner {

    /**
     * 对请求进行签名，并将时间戳、随机数和签名写入请求头。
     */
    void sign(ApiRequest request, ApiConfig config);
}
