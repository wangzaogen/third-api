package com.thirdapi.starter.auth;

import com.thirdapi.sdk.core.model.AuthType;
import com.thirdapi.starter.config.ApiConfig;
import com.thirdapi.starter.http.ApiRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Applies auth headers to a request based on runtime channel config.
 */
public class AuthProcessor {

    private final TokenProvider tokenProvider;
    private final ApiSigner apiSigner;

    public AuthProcessor(TokenProvider tokenProvider, ApiSigner apiSigner) {
        this.tokenProvider = tokenProvider;
        this.apiSigner = apiSigner;
    }

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

    private void applyApiKey(ApiRequest request, ApiConfig config) {
        String header = "X-Api-Key";
        Map<String, String> extra = config.getExtraAuthConfig();
        if (extra != null && extra.get("header") != null && !extra.get("header").isEmpty()) {
            header = extra.get("header");
        }
        request.addHeader(header, config.getApiKey() == null ? "" : config.getApiKey());
    }

    private void applyBasic(ApiRequest request, ApiConfig config) {
        String raw = (config.getClientId() == null ? "" : config.getClientId())
                + ":" + (config.getClientSecret() == null ? "" : config.getClientSecret());
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Basic " + encoded);
    }
}
