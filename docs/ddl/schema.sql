-- third-api admin console schema
-- Target: MySQL 8.x

CREATE DATABASE IF NOT EXISTS third_api
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE third_api;

CREATE TABLE api_app (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    app_id VARCHAR(128) NOT NULL,
    app_name VARCHAR(128) NOT NULL,
    app_secret_encrypted VARCHAR(512) DEFAULT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(512) DEFAULT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='business application registered to third-api';

CREATE TABLE api_provider (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL COMMENT 'unique provider code, e.g. sms',
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'OTHER',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(512) DEFAULT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='third party provider';

CREATE TABLE api_channel (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    provider_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(64) NOT NULL COMMENT 'unique channel code, e.g. aliyun-sms',
    name VARCHAR(128) NOT NULL,
    base_url VARCHAR(512) NOT NULL,
    environment VARCHAR(32) NOT NULL DEFAULT 'prod' COMMENT 'dev/test/prod',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'ONLINE' COMMENT 'ONLINE/OFFLINE/MAINTENANCE',
    priority INT NOT NULL DEFAULT 100,
    health_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN/UP/DEGRADED/DOWN',
    last_health_at DATETIME DEFAULT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_channel_code (code),
    KEY idx_channel_provider (provider_id),
    CONSTRAINT fk_channel_provider FOREIGN KEY (provider_id) REFERENCES api_provider (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='channel instance of a provider';

CREATE TABLE api_app_channel (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    app_id BIGINT UNSIGNED NOT NULL,
    channel_id BIGINT UNSIGNED NOT NULL,
    priority INT NOT NULL DEFAULT 100,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_channel (app_id, channel_id),
    KEY idx_app_channel_channel (channel_id),
    CONSTRAINT fk_app_channel_app FOREIGN KEY (app_id) REFERENCES api_app (id),
    CONSTRAINT fk_app_channel_channel FOREIGN KEY (channel_id) REFERENCES api_channel (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='channel binding for a business application';

CREATE TABLE api_endpoint (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    channel_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    http_method VARCHAR(16) NOT NULL DEFAULT 'POST',
    path VARCHAR(512) NOT NULL,
    content_type VARCHAR(64) NOT NULL DEFAULT 'application/json',
    timeout_ms INT NOT NULL DEFAULT 5000,
    retry_max INT NOT NULL DEFAULT 2,
    retry_backoff_ms BIGINT NOT NULL DEFAULT 200,
    circuit_breaker_ratio DECIMAL(5,2) NOT NULL DEFAULT 0.50,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_endpoint_channel_code (channel_id, code),
    CONSTRAINT fk_endpoint_channel FOREIGN KEY (channel_id) REFERENCES api_channel (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='third-party endpoint definition';

CREATE TABLE api_policy (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    scope VARCHAR(32) NOT NULL DEFAULT 'ENDPOINT' COMMENT 'GLOBAL/PROVIDER/CHANNEL/ENDPOINT',
    target_type VARCHAR(32) NOT NULL DEFAULT 'ENDPOINT',
    target_id BIGINT UNSIGNED NOT NULL,
    timeout_ms INT DEFAULT NULL,
    retry_max INT DEFAULT NULL,
    retry_backoff_ms BIGINT DEFAULT NULL,
    circuit_breaker_enabled TINYINT(1) NOT NULL DEFAULT 1,
    circuit_breaker_ratio DECIMAL(5,2) DEFAULT NULL,
    rate_limit_per_second INT DEFAULT NULL,
    bulkhead_max_concurrent INT DEFAULT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_policy_scope_target (scope, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='retry/timeout/circuit breaker policy';

CREATE TABLE api_auth_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    channel_id BIGINT UNSIGNED NOT NULL,
    auth_type VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/API_KEY/BASIC/OAUTH2/SIGN',
    token_url VARCHAR(512) DEFAULT NULL,
    client_id VARCHAR(256) DEFAULT NULL,
    client_secret_encrypted VARCHAR(512) DEFAULT NULL,
    extra_config JSON DEFAULT NULL,
    token_cache_ttl_seconds INT NOT NULL DEFAULT 300,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_channel (channel_id),
    CONSTRAINT fk_auth_channel FOREIGN KEY (channel_id) REFERENCES api_channel (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='auth and secret configuration';

CREATE TABLE api_config_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    config_type VARCHAR(32) NOT NULL COMMENT 'PROVIDER/CHANNEL/ENDPOINT/POLICY/AUTH',
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT UNSIGNED NOT NULL,
    version BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/GRAY/PUBLISHED/ROLLED_BACK',
    gray_percent INT NOT NULL DEFAULT 0,
    config_json JSON NOT NULL,
    published_by VARCHAR(128) DEFAULT NULL,
    published_at DATETIME DEFAULT NULL,
    rolled_back_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_version_target (config_type, target_type, target_id, version),
    KEY idx_config_version_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='config version, gray release and rollback';

CREATE TABLE api_call_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL,
    app_name VARCHAR(128) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    channel_code VARCHAR(64) NOT NULL,
    endpoint_code VARCHAR(64) NOT NULL,
    http_method VARCHAR(16) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    request_body TEXT,
    response_body TEXT,
    http_status INT DEFAULT NULL,
    biz_code VARCHAR(64) DEFAULT NULL,
    success TINYINT(1) NOT NULL DEFAULT 1,
    cost_ms INT NOT NULL DEFAULT 0,
    error_type VARCHAR(64) DEFAULT NULL,
    error_message VARCHAR(1024) DEFAULT NULL,
    request_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_call_log_trace (trace_id),
    KEY idx_call_log_provider_time (provider_code, request_at),
    KEY idx_call_log_channel_time (channel_code, request_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='standardized third-party call log';

CREATE TABLE api_health_check (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    channel_id BIGINT UNSIGNED NOT NULL,
    endpoint_id BIGINT UNSIGNED DEFAULT NULL,
    check_type VARCHAR(32) NOT NULL DEFAULT 'HTTP' COMMENT 'HTTP/TEST_CALL',
    target_url VARCHAR(1024) DEFAULT NULL,
    success TINYINT(1) NOT NULL DEFAULT 0,
    status_code INT DEFAULT NULL,
    cost_ms INT DEFAULT NULL,
    error_message VARCHAR(1024) DEFAULT NULL,
    checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_health_channel_time (channel_id, checked_at),
    CONSTRAINT fk_health_channel FOREIGN KEY (channel_id) REFERENCES api_channel (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='health check result';

CREATE TABLE api_audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    operator VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT UNSIGNED NOT NULL,
    before_json JSON DEFAULT NULL,
    after_json JSON DEFAULT NULL,
    client_ip VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_target (target_type, target_id, created_at),
    KEY idx_audit_operator (operator, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='admin operation audit log';
