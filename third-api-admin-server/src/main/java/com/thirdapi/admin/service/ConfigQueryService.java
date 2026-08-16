package com.thirdapi.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdapi.admin.dto.ApiConfigDto;
import com.thirdapi.admin.dto.ConfigSnapshotDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class ConfigQueryService {

    private static final String CONFIG_SQL =
            "SELECT p.code AS provider_code, "
                    + "c.code AS channel_code, "
                    + "e.code AS endpoint_code, "
                    + "c.base_url, "
                    + "e.path, "
                    + "e.http_method, "
                    + "COALESCE(pol.timeout_ms, e.timeout_ms) AS timeout_ms, "
                    + "COALESCE(pol.retry_max, e.retry_max) AS retry_max, "
                    + "COALESCE(pol.retry_backoff_ms, e.retry_backoff_ms) AS retry_backoff_ms, "
                    + "COALESCE(pol.circuit_breaker_ratio, e.circuit_breaker_ratio) AS breaker_ratio, "
                    + "a.auth_type, "
                    + "a.token_url, "
                    + "a.client_id, "
                    + "a.client_secret_encrypted, "
                    + "a.extra_config "
                    + "FROM api_app app "
                    + "JOIN api_app_channel ac ON ac.app_id = app.id AND ac.enabled = 1 "
                    + "JOIN api_channel c ON c.id = ac.channel_id AND c.enabled = 1 "
                    + "JOIN api_provider p ON p.id = c.provider_id "
                    + "JOIN api_endpoint e ON e.channel_id = c.id AND e.enabled = 1 "
                    + "LEFT JOIN api_policy pol ON pol.scope = 'ENDPOINT' AND pol.target_type = 'ENDPOINT' "
                    + "  AND pol.target_id = e.id AND pol.enabled = 1 "
                    + "LEFT JOIN api_auth_config a ON a.channel_id = c.id AND a.enabled = 1 "
                    + "WHERE app.app_id = ? "
                    + "ORDER BY p.code, c.code, e.code";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ConfigQueryService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Long requireAppId(String appId) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM api_app WHERE app_id = ? AND enabled = 1", Long.class, appId);
        if (ids.isEmpty()) {
            throw new AppNotFoundException(appId);
        }
        return ids.get(0);
    }

    public long currentVersion(String appId) {
        Long version = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(v.version), 0) "
                        + "FROM api_config_version v "
                        + "JOIN api_app a ON v.target_type = 'APP' AND v.target_id = a.id "
                        + "WHERE v.status = 'PUBLISHED' AND a.app_id = ?",
                Long.class, appId);
        return version == null ? 0L : version;
    }

    public ConfigSnapshotDto loadCurrentSnapshot(String appId) {
        requireAppId(appId);
        ConfigSnapshotDto snapshot = new ConfigSnapshotDto();
        snapshot.setVersion(currentVersion(appId));
        snapshot.setConfigs(jdbcTemplate.query(CONFIG_SQL, this::mapConfig, appId));
        return snapshot;
    }

    private ApiConfigDto mapConfig(ResultSet rs, int rowNum) throws SQLException {
        ApiConfigDto config = new ApiConfigDto();
        config.setProvider(rs.getString("provider_code"));
        config.setChannel(rs.getString("channel_code"));
        config.setEndpoint(rs.getString("endpoint_code"));
        config.setBaseUrl(rs.getString("base_url"));
        config.setPath(rs.getString("path"));
        config.setHttpMethod(rs.getString("http_method") == null ? "POST" : rs.getString("http_method").toUpperCase());
        config.setTimeoutMs(rs.getInt("timeout_ms"));
        config.setMaxRetries(rs.getInt("retry_max"));
        config.setRetryBackoffMs(rs.getLong("retry_backoff_ms"));
        double ratio = rs.getDouble("breaker_ratio");
        config.setCircuitBreakerThreshold((int) Math.round(ratio * 100));
        config.setAuthType(rs.getString("auth_type") == null ? "NONE" : rs.getString("auth_type").toUpperCase());
        config.setTokenUrl(rs.getString("token_url"));
        config.setClientId(rs.getString("client_id"));
        config.setClientSecret(rs.getString("client_secret_encrypted"));
        config.setApiKey(extractApiKey(rs.getString("extra_config")));
        return config;
    }

    private String extractApiKey(String extraConfig) {
        if (extraConfig == null || extraConfig.isEmpty()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(extraConfig);
            return node.has("apiKey") && !node.get("apiKey").isNull()
                    ? node.get("apiKey").asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
