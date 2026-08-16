package com.thirdapi.starter.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdapi.starter.config.ApiConfig;
import com.thirdapi.starter.util.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * OAuth2 client_credentials 令牌提供者，带缓存和并发刷新保护。
 */
public class OAuth2TokenProvider implements TokenProvider {

    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, TokenEntry> cache = new ConcurrentHashMap<String, TokenEntry>();

    public OAuth2TokenProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getToken(ApiConfig config) {
        String key = config.key();
        TokenEntry entry = cache.get(key);
        if (entry != null && entry.isValid()) {
            return entry.getToken();
        }
        // 双重检查加锁，避免令牌过期时多个线程同时请求刷新
        synchronized (cache) {
            entry = cache.get(key);
            if (entry != null && entry.isValid()) {
                return entry.getToken();
            }
            TokenEntry refreshed = refresh(config);
            cache.put(key, refreshed);
            return refreshed.getToken();
        }
    }

    /**
     * 调用 tokenUrl 以 client_credentials 方式换取 access_token。
     */
    private TokenEntry refresh(ApiConfig config) {
        if (config.getTokenUrl() == null || config.getClientId() == null || config.getClientSecret() == null) {
            throw new IllegalStateException("OAuth2 config missing tokenUrl/clientId/clientSecret for " + config.key());
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(config.getTokenUrl()).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            String body = "grant_type=" + encode("client_credentials")
                    + "&client_id=" + encode(config.getClientId())
                    + "&client_secret=" + encode(config.getClientSecret());
            OutputStream output = connection.getOutputStream();
            output.write(body.getBytes(StandardCharsets.UTF_8));
            output.flush();
            output.close();
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Token request failed with status " + status);
            }
            InputStream input = connection.getInputStream();
            JsonNode json = objectMapper.readTree(Streams.readUtf8(input));
            JsonNode tokenNode = json.get("access_token");
            if (tokenNode == null) {
                throw new IllegalStateException("Token response has no access_token");
            }
            long expiresIn = json.has("expires_in") ? json.get("expires_in").asLong(3600) : 3600L;
            // 提前 60 秒过期，避免令牌刚写入就失效
            return new TokenEntry(tokenNode.asText(), System.currentTimeMillis() + (expiresIn - 60) * 1000L);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh token for " + config.key(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String encode(String value) throws IOException {
        return URLEncoder.encode(value, "UTF-8");
    }

    /**
     * 缓存中的令牌条目，包含令牌值与过期时间。
     */
    private static class TokenEntry {

        private final String token;
        private final long expiresAt;

        private TokenEntry(String token, long expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }

        /**
         * 令牌尚未过期即视为有效。
         */
        private boolean isValid() {
            return expiresAt > System.currentTimeMillis();
        }

        private String getToken() {
            return token;
        }
    }
}
