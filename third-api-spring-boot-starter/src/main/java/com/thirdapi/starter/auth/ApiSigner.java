package com.thirdapi.starter.auth;

import com.thirdapi.starter.config.ApiConfig;
import com.thirdapi.starter.http.ApiRequest;

public interface ApiSigner {

    void sign(ApiRequest request, ApiConfig config);
}
