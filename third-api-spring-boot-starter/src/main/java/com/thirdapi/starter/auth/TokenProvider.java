package com.thirdapi.starter.auth;

import com.thirdapi.starter.config.ApiConfig;

public interface TokenProvider {

    String getToken(ApiConfig config);
}
