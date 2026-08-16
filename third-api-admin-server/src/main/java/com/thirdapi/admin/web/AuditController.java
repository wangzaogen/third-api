package com.thirdapi.admin.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditController {

    private final JdbcTemplate jdbcTemplate;

    public AuditController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "100") int limit) {
        return jdbcTemplate.queryForList(
                "SELECT id, operator, action, target_type AS targetType, target_id AS targetId, "
                        + "before_json AS beforeJson, after_json AS afterJson, created_at AS createdAt "
                        + "FROM api_audit_log ORDER BY id DESC LIMIT ?",
                Math.max(1, Math.min(limit, 500)));
    }
}
