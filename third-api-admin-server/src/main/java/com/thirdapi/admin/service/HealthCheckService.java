package com.thirdapi.admin.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HealthCheckService {

    private final JdbcTemplate jdbcTemplate;

    public HealthCheckService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> list(Long channelId) {
        return jdbcTemplate.queryForList(
                "SELECT id, channel_id AS channelId, endpoint_id AS endpointId, check_type AS checkType, "
                        + "target_url AS targetUrl, success, status_code AS statusCode, cost_ms AS costMs, "
                        + "error_message AS errorMessage, checked_at AS checkedAt "
                        + "FROM api_health_check "
                        + "WHERE (? IS NULL OR channel_id = ?) "
                        + "ORDER BY id DESC LIMIT 100",
                channelId, channelId);
    }

    public List<Map<String, Object>> run(Long channelId) {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        Map<String, Object> channel = jdbcTemplate.queryForMap(
                "SELECT id, base_url AS baseUrl FROM api_channel WHERE id = ?", channelId);
        String baseUrl = String.valueOf(channel.get("baseUrl"));
        List<Map<String, Object>> endpoints = jdbcTemplate.queryForList(
                "SELECT id, path FROM api_endpoint WHERE channel_id = ? AND enabled = 1", channelId);
        if (endpoints.isEmpty()) {
            results.add(runOne(channelId, null, baseUrl, baseUrl));
        } else {
            for (Map<String, Object> endpoint : endpoints) {
                Long endpointId = ((Number) endpoint.get("id")).longValue();
                String path = String.valueOf(endpoint.get("path"));
                String target = join(baseUrl, path);
                results.add(runOne(channelId, endpointId, target, baseUrl));
            }
        }
        return results;
    }

    private Map<String, Object> runOne(Long channelId, Long endpointId, String targetUrl, String baseUrl) {
        HttpURLConnection connection = null;
        long startedAt = System.currentTimeMillis();
        int status = 0;
        String error = null;
        try {
            connection = (HttpURLConnection) new URL(targetUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            status = connection.getResponseCode();
        } catch (IOException e) {
            error = e.getMessage();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        long cost = System.currentTimeMillis() - startedAt;
        boolean success = status >= 200 && status < 300;
        jdbcTemplate.update(
                "INSERT INTO api_health_check "
                        + "(channel_id, endpoint_id, check_type, target_url, success, status_code, cost_ms, error_message, checked_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                channelId, endpointId, "HTTP", targetUrl, success, status == 0 ? null : status,
                cost, error);
        Map<String, Object> result = new java.util.HashMap<String, Object>();
        result.put("channelId", channelId);
        result.put("endpointId", endpointId);
        result.put("targetUrl", targetUrl);
        result.put("success", success);
        result.put("statusCode", status == 0 ? null : status);
        result.put("costMs", cost);
        result.put("errorMessage", error);
        return result;
    }

    private String join(String base, String path) {
        String b = base == null ? "" : base;
        String p = path == null ? "" : path;
        if (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b + (p.startsWith("/") ? p : "/" + p);
    }
}
