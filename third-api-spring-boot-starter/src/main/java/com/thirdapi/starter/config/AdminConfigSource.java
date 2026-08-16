package com.thirdapi.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdapi.starter.autoconfigure.ThirdApiProperties;
import com.thirdapi.starter.util.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 从管理端拉取配置的配置源。
 *
 * <p>协议：GET /api/v1/apps/{appId}/configs?version={version}&amp;longPoll={seconds}
 * 请求头携带 X-App-Id 与 X-App-Secret；服务端返回 304 表示配置没有变化。</p>
 */
public class AdminConfigSource implements ConfigSource {

    private static final Logger log = LoggerFactory.getLogger(AdminConfigSource.class);

    private final ObjectMapper objectMapper;
    private final ThirdApiProperties properties;
    private volatile long lastVersion;

    public AdminConfigSource(ObjectMapper objectMapper, ThirdApiProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public String name() {
        return "admin";
    }

    @Override
    public ConfigSnapshot load() {
        HttpURLConnection connection = null;
        try {
            String url = buildUrl();
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(properties.getLongPollTimeoutSeconds() * 1000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-App-Id", properties.getAppId());
            connection.setRequestProperty("X-App-Secret", properties.getAppSecret());
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
                // 版本未变化，返回 null 表示本轮无需更新
                return null;
            }
            if (status != HttpURLConnection.HTTP_OK) {
                log.warn("Admin config request failed with status {}", status);
                return null;
            }
            InputStream input = connection.getInputStream();
            String body = Streams.readUtf8(input);
            if (body == null || body.isEmpty()) {
                return null;
            }
            ConfigSnapshot snapshot = objectMapper.readValue(body, ConfigSnapshot.class);
            lastVersion = snapshot.getVersion();
            return snapshot;
        } catch (IOException e) {
            log.warn("Failed to pull config from admin: {}", e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 拼接管理端配置拉取地址，版本号与长轮询秒数来自属性配置。
     */
    private String buildUrl() throws IOException {
        String base = properties.getAdminUrl();
        if (base == null || base.isEmpty()) {
            throw new IOException("third-api.admin-url is empty");
        }
        StringBuilder sb = new StringBuilder(base);
        if (sb.charAt(sb.length() - 1) == '/') {
            sb.setLength(sb.length() - 1);
        }
        sb.append("/api/v1/apps/").append(properties.getAppId())
                .append("/configs?version=").append(lastVersion)
                .append("&longPoll=").append(properties.getLongPollTimeoutSeconds());
        return sb.toString();
    }
}
