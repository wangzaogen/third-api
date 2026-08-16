package com.thirdapi.starter.auth;

import com.thirdapi.sdk.core.model.AuthType;
import com.thirdapi.starter.config.ApiConfig;
import com.thirdapi.starter.http.ApiRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 根据渠道运行时配置为请求附加对应的鉴权信息。
 */
public class AuthProcessor {

    private final TokenProvider tokenProvider;
    private final ApiSigner apiSigner;

    public AuthProcessor(TokenProvider tokenProvider, ApiSigner apiSigner) {
        this.tokenProvider = tokenProvider;
        this.apiSigner = apiSigner;
    }

    /**
     * 按配置中的鉴权类型分发到具体的鉴权实现。
     */
    public void apply(ApiRequest request, ApiConfig config) {
        AuthType type = AuthType.valueOf(config.getAuthType() == null ? "NONE" : config.getAuthType().toUpperCase());
        switch (type) {
            case API_KEY:
                applyApiKey(request, config);
                break;
            case BASIC:
                applyBasic(request, config);
                break;
            case OAUTH2:
                request.addHeader("Authorization", "Bearer " + tokenProvider.getToken(config));
                break;
            case SIGN:
                apiSigner.sign(request, config);
                break;
            case NONE:
            default:
                break;
        }
    }

    /**
     * 默认写入 X-Api-Key 请求头，也可通过 extraAuthConfig.header 自定义请求头名。
     */
    private void applyApiKey(ApiRequest request, ApiConfig config) {
        String header = "X-Api-Key";
        Map<String, String> extra = config.getExtraAuthConfig();
        if (extra != null && extra.get("header") != null && !extra.get("header").isEmpty()) {
            header = extra.get("header");
        }
        request.addHeader(header, config.getApiKey() == null ? "" : config.getApiKey());
    }

    /**
     * 将 clientId:clientSecret 做 Base64 编码后写入 Authorization。
     */
    private void applyBasic(ApiRequest request, ApiConfig config) {
        String raw = (config.getClientId() == null ? "" : config.getClientId())
                + ":" + (config.getClientSecret() == null ? "" : config.getClientSecret());
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Basic " + encoded);
    }
}
