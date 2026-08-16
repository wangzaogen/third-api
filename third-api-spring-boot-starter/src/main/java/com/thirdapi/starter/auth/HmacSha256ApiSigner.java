package com.thirdapi.starter.auth;

import com.thirdapi.starter.config.ApiConfig;
import com.thirdapi.starter.http.ApiRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class HmacSha256ApiSigner implements ApiSigner {

    @Override
    public void sign(ApiRequest request, ApiConfig config) {
        String secret = config.getClientSecret() != null ? config.getClientSecret() : config.getApiKey();
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("SIGN auth requires clientSecret or apiKey for " + config.key());
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String payload = timestamp + "\n" + request.getMethod() + "\n" + request.getUrl()
                + "\n" + (request.getBody() == null ? "" : request.getBody());
        String signature = hmacSha256(secret, payload);
        request.addHeader("X-Timestamp", timestamp);
        request.addHeader("X-Nonce", nonce);
        request.addHeader("X-Signature", signature);
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            request.addHeader("X-Api-Key", config.getApiKey());
        }
    }

    private String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign request", e);
        }
    }
}
