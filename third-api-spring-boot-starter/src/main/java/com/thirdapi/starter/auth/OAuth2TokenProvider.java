package com.thirdapi.starter.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdapi.starter.config.ApiConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * OAuth2 client-credentials token provider with cache and single-flight refresh.
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
            JsonNode json = objectMapper.readTree(read(input));
            JsonNode tokenNode = json.get("access_token");
            if (tokenNode == null || tokenNode.isMissingNode()) {
                throw new IllegalStateException("Token response has no access_token");
            }
            long expiresIn = json.has("expires_in") ? json.get("expires_in").asLong(3600) : 3600L;
            return new TokenEntry(tokenNode.asText(), System.currentTimeMillis() + (expiresIn - 60) * 1000L);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh token for " + config.key(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String encode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    private String read(InputStream input) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static class TokenEntry {

        private final String token;
        private final long expiresAt;

        private TokenEntry(String token, long expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }

        private boolean isValid() {
            return expiresAt > System.currentTimeMillis();
        }

        private String getToken() {
            return token;
        }
    }
}
