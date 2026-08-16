package com.thirdapi.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdapi.admin.dto.ConfigSnapshotDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class ConfigPublishService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ConfigQueryService configQueryService;
    private final AuditLogService auditLogService;

    public ConfigPublishService(JdbcTemplate jdbcTemplate,
                                ObjectMapper objectMapper,
                                ConfigQueryService configQueryService,
                                AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.configQueryService = configQueryService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ConfigSnapshotDto publish(String appId, String operator) {
        Long appRowId = configQueryService.requireAppId(appId);
        ConfigSnapshotDto snapshot = configQueryService.loadCurrentSnapshot(appId);
        long nextVersion = configQueryService.currentVersion(appId) + 1;
        snapshot.setVersion(nextVersion);
        try {
            String configJson = objectMapper.writeValueAsString(snapshot);
            jdbcTemplate.update(
                    "INSERT INTO api_config_version "
                            + "(config_type, target_type, target_id, version, status, gray_percent, config_json, published_by, published_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    "APP", "APP", appRowId, nextVersion, "PUBLISHED", 100, configJson, operator);
            auditLogService.record(operator, "PUBLISH", "APP", appRowId, null,
                    Collections.singletonMap("version", nextVersion));
            return snapshot;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish config for app " + appId, e);
        }
    }
}
