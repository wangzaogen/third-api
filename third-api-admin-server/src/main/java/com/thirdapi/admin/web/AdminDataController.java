package com.thirdapi.admin.web;

import com.thirdapi.admin.service.AuditLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Minimal admin CRUD for apps, providers, channels, endpoints and channel bindings.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminDataController {

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;
    private final boolean sqlite;

    public AdminDataController(JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
        this.sqlite = isSqlite(jdbcTemplate);
    }

    @GetMapping("/apps")
    public List<Map<String, Object>> listApps() {
        return jdbcTemplate.queryForList(
                "SELECT id, app_id AS appId, app_name AS appName, "
                        + "app_secret_encrypted AS appSecret, enabled, remark "
                        + "FROM api_app ORDER BY id");
    }

    @PostMapping("/apps")
    public Map<String, Object> createApp(@RequestBody Map<String, Object> body) {
        long id = insert("api_app",
                "app_id, app_name, app_secret_encrypted, enabled, remark",
                str(body, "appId"), str(body, "appName"), str(body, "appSecret"),
                bool(body, "enabled", true), str(body, "remark"));
        auditLogService.record("admin", "CREATE", "APP", id, null, body);
        return Collections.singletonMap("id", id);
    }

    @PutMapping("/apps/{id}")
    public void updateApp(@PathVariable long id, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
                "UPDATE api_app SET app_name = ?, app_secret_encrypted = ?, enabled = ?, remark = ? WHERE id = ?",
                str(body, "appName"), str(body, "appSecret"), bool(body, "enabled", true),
                str(body, "remark"), id);
        auditLogService.record("admin", "UPDATE", "APP", id, null, body);
    }

    @DeleteMapping("/apps/{id}")
    public void deleteApp(@PathVariable long id) {
        jdbcTemplate.update("DELETE FROM api_app_channel WHERE app_id = ?", id);
        jdbcTemplate.update("DELETE FROM api_app WHERE id = ?", id);
        auditLogService.record("admin", "DELETE", "APP", id, null, null);
    }

    @GetMapping("/apps/{id}/channels")
    public List<Long> listAppChannels(@PathVariable long id) {
        return jdbcTemplate.queryForList(
                "SELECT channel_id FROM api_app_channel WHERE app_id = ? ORDER BY priority", Long.class, id);
    }

    @PostMapping("/apps/{id}/channels/{channelId}")
    public void bindChannel(@PathVariable long id, @PathVariable long channelId) {
        String sql = sqlite
                ? "INSERT OR IGNORE INTO api_app_channel (app_id, channel_id, priority) VALUES (?, ?, 100)"
                : "INSERT IGNORE INTO api_app_channel (app_id, channel_id, priority) VALUES (?, ?, 100)";
        jdbcTemplate.update(sql, id, channelId);
        auditLogService.record("admin", "BIND_CHANNEL", "APP", id, null,
                Collections.singletonMap("channelId", channelId));
    }

    @DeleteMapping("/apps/{id}/channels/{channelId}")
    public void unbindChannel(@PathVariable long id, @PathVariable long channelId) {
        jdbcTemplate.update("DELETE FROM api_app_channel WHERE app_id = ? AND channel_id = ?", id, channelId);
        auditLogService.record("admin", "UNBIND_CHANNEL", "APP", id, null,
                Collections.singletonMap("channelId", channelId));
    }

    @GetMapping("/providers")
    public List<Map<String, Object>> listProviders() {
        return jdbcTemplate.queryForList(
                "SELECT id, code, name, category, enabled, remark FROM api_provider ORDER BY id");
    }

    @PostMapping("/providers")
    public Map<String, Object> createProvider(@RequestBody Map<String, Object> body) {
        long id = insert("api_provider", "code, name, category, enabled, remark",
                str(body, "code"), str(body, "name"), str(body, "category", "OTHER"),
                bool(body, "enabled", true), str(body, "remark"));
        auditLogService.record("admin", "CREATE", "PROVIDER", id, null, body);
        return Collections.singletonMap("id", id);
    }

    @PutMapping("/providers/{id}")
    public void updateProvider(@PathVariable long id, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
                "UPDATE api_provider SET code = ?, name = ?, category = ?, enabled = ?, remark = ? WHERE id = ?",
                str(body, "code"), str(body, "name"), str(body, "category", "OTHER"),
                bool(body, "enabled", true), str(body, "remark"), id);
        auditLogService.record("admin", "UPDATE", "PROVIDER", id, null, body);
    }

    @DeleteMapping("/providers/{id}")
    public void deleteProvider(@PathVariable long id) {
        List<Long> channelIds = jdbcTemplate.queryForList(
                "SELECT id FROM api_channel WHERE provider_id = ?", Long.class, id);
        for (Long channelId : channelIds) {
            deleteChannel(channelId);
        }
        jdbcTemplate.update("DELETE FROM api_provider WHERE id = ?", id);
        auditLogService.record("admin", "DELETE", "PROVIDER", id, null, null);
    }

    @GetMapping("/channels")
    public List<Map<String, Object>> listChannels() {
        return jdbcTemplate.queryForList(
                "SELECT c.id, c.provider_id AS providerId, p.code AS providerCode, "
                        + "c.code, c.name, c.base_url AS baseUrl, c.environment, c.enabled, c.priority "
                        + "FROM api_channel c JOIN api_provider p ON p.id = c.provider_id ORDER BY c.id");
    }

    @PostMapping("/channels")
    public Map<String, Object> createChannel(@RequestBody Map<String, Object> body) {
        long id = insert("api_channel",
                "provider_id, code, name, base_url, environment, enabled, priority",
                longVal(body, "providerId"), str(body, "code"), str(body, "name"),
                str(body, "baseUrl"), str(body, "environment", "prod"),
                bool(body, "enabled", true), intVal(body, "priority", 100));
        auditLogService.record("admin", "CREATE", "CHANNEL", id, null, body);
        return Collections.singletonMap("id", id);
    }

    @PutMapping("/channels/{id}")
    public void updateChannel(@PathVariable long id, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
                "UPDATE api_channel SET provider_id = ?, code = ?, name = ?, base_url = ?, "
                        + "environment = ?, enabled = ?, priority = ? WHERE id = ?",
                longVal(body, "providerId"), str(body, "code"), str(body, "name"),
                str(body, "baseUrl"), str(body, "environment", "prod"),
                bool(body, "enabled", true), intVal(body, "priority", 100), id);
        auditLogService.record("admin", "UPDATE", "CHANNEL", id, null, body);
    }

    @DeleteMapping("/channels/{id}")
    public void deleteChannel(@PathVariable long id) {
        jdbcTemplate.update("DELETE FROM api_auth_config WHERE channel_id = ?", id);
        jdbcTemplate.update("DELETE FROM api_endpoint WHERE channel_id = ?", id);
        jdbcTemplate.update("DELETE FROM api_app_channel WHERE channel_id = ?", id);
        jdbcTemplate.update("DELETE FROM api_channel WHERE id = ?", id);
        auditLogService.record("admin", "DELETE", "CHANNEL", id, null, null);
    }

    @GetMapping("/channels/{id}/endpoints")
    public List<Map<String, Object>> listEndpoints(@PathVariable long id) {
        return jdbcTemplate.queryForList(
                "SELECT id, channel_id AS channelId, code, name, http_method AS httpMethod, path, "
                        + "content_type AS contentType, timeout_ms AS timeoutMs, retry_max AS retryMax, "
                        + "retry_backoff_ms AS retryBackoffMs, circuit_breaker_ratio AS circuitBreakerRatio, enabled "
                        + "FROM api_endpoint WHERE channel_id = ? ORDER BY id", id);
    }

    @PostMapping("/channels/{channelId}/endpoints")
    public Map<String, Object> createEndpoint(@PathVariable long channelId,
                                              @RequestBody Map<String, Object> body) {
        long id = insert("api_endpoint",
                "channel_id, code, name, http_method, path, content_type, timeout_ms, "
                        + "retry_max, retry_backoff_ms, circuit_breaker_ratio, enabled",
                channelId, str(body, "code"), str(body, "name"),
                str(body, "httpMethod", "POST"), str(body, "path"),
                str(body, "contentType", "application/json"), intVal(body, "timeoutMs", 5000),
                intVal(body, "retryMax", 2), longVal(body, "retryBackoffMs", 200L),
                doubleVal(body, "circuitBreakerRatio", 0.50), bool(body, "enabled", true));
        auditLogService.record("admin", "CREATE", "ENDPOINT", id, null, body);
        return Collections.singletonMap("id", id);
    }

    @PutMapping("/endpoints/{id}")
    public void updateEndpoint(@PathVariable long id, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
                "UPDATE api_endpoint SET code = ?, name = ?, http_method = ?, path = ?, content_type = ?, "
                        + "timeout_ms = ?, retry_max = ?, retry_backoff_ms = ?, circuit_breaker_ratio = ?, enabled = ? "
                        + "WHERE id = ?",
                str(body, "code"), str(body, "name"), str(body, "httpMethod", "POST"),
                str(body, "path"), str(body, "contentType", "application/json"),
                intVal(body, "timeoutMs", 5000), intVal(body, "retryMax", 2),
                longVal(body, "retryBackoffMs", 200L), doubleVal(body, "circuitBreakerRatio", 0.50),
                bool(body, "enabled", true), id);
        auditLogService.record("admin", "UPDATE", "ENDPOINT", id, null, body);
    }

    @DeleteMapping("/endpoints/{id}")
    public void deleteEndpoint(@PathVariable long id) {
        jdbcTemplate.update("DELETE FROM api_endpoint WHERE id = ?", id);
        auditLogService.record("admin", "DELETE", "ENDPOINT", id, null, null);
    }

    private long insert(String table, String columns, Object... values) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" (").append(columns)
                .append(") VALUES (");
        for (int i = 0; i < values.length; i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")");
        KeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            return ps;
        }, holder);
        Number key = holder.getKey();
        return key == null ? 0L : key.longValue();
    }

    private String str(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String str(Map<String, Object> map, String key, String defaultValue) {
        String value = str(map, key);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    private boolean bool(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(String.valueOf(value));
    }

    private int intVal(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
    }

    private long longVal(Map<String, Object> map, String key) {
        return Long.parseLong(String.valueOf(map.get(key)));
    }

    private long longVal(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : Long.parseLong(String.valueOf(value));
    }

    private double doubleVal(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : Double.parseDouble(String.valueOf(value));
    }

    private boolean isSqlite(JdbcTemplate jdbcTemplate) {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("sqlite");
        } catch (SQLException e) {
            return false;
        }
    }
}
