package com.thirdapi.admin.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/channels/{channelId}/auth")
public class AuthConfigController {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AuthConfigController(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public Map<String, Object> get(@PathVariable long channelId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, channel_id AS channelId, auth_type AS authType, token_url AS tokenUrl, "
                        + "client_id AS clientId, client_secret_encrypted AS clientSecret, "
                        + "extra_config AS extraConfig, token_cache_ttl_seconds AS tokenCacheTtlSeconds, enabled "
                        + "FROM api_auth_config WHERE channel_id = ?",
                channelId);
        return rows.isEmpty() ? Collections.emptyMap() : rows.get(0);
    }

    @PutMapping
    public Map<String, Object> upsert(@PathVariable long channelId,
                                      @RequestBody Map<String, Object> body) throws Exception {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM api_auth_config WHERE channel_id = ?", Long.class, channelId);
        String extraConfig = body.get("extraConfig") == null ? null
                : objectMapper.writeValueAsString(body.get("extraConfig"));
        String authType = str(body.get("authType"), "NONE");
        int ttl = intVal(body.get("tokenCacheTtlSeconds"), 300);
        boolean enabled = bool(body.get("enabled"), true);
        if (ids.isEmpty()) {
            KeyHolder holder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO api_auth_config "
                                + "(channel_id, auth_type, token_url, client_id, client_secret_encrypted, "
                                + "extra_config, token_cache_ttl_seconds, enabled) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, channelId);
                ps.setString(2, authType);
                ps.setString(3, str(body.get("tokenUrl"), null));
                ps.setString(4, str(body.get("clientId"), null));
                ps.setString(5, str(body.get("clientSecret"), null));
                ps.setString(6, extraConfig);
                ps.setInt(7, ttl);
                ps.setBoolean(8, enabled);
                return ps;
            }, holder);
            return Collections.singletonMap("id", holder.getKey().longValue());
        }
        jdbcTemplate.update(
                "UPDATE api_auth_config SET auth_type = ?, token_url = ?, client_id = ?, "
                        + "client_secret_encrypted = ?, extra_config = ?, token_cache_ttl_seconds = ?, enabled = ? "
                        + "WHERE id = ?",
                authType, str(body.get("tokenUrl"), null), str(body.get("clientId"), null),
                str(body.get("clientSecret"), null), extraConfig, ttl, enabled, ids.get(0));
        return Collections.singletonMap("id", ids.get(0));
    }

    private String str(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int intVal(Object value, int defaultValue) {
        return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
    }

    private boolean bool(Object value, boolean defaultValue) {
        return value == null ? defaultValue : Boolean.parseBoolean(String.valueOf(value));
    }
}
