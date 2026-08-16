INSERT INTO api_app (id, app_id, app_name, app_secret_encrypted, enabled, remark)
VALUES (1, 'order-service', '订单服务', 'secret', 1, 'SQLite E2E seed');

INSERT INTO api_provider (id, code, name, category, enabled, remark)
VALUES (1, 'sms', '短信服务', 'NOTIFY', 1, 'SQLite E2E seed');

INSERT INTO api_channel (id, provider_id, code, name, base_url, environment, enabled, priority)
VALUES (1, 1, 'sqlite-sms', 'SQLite 短信渠道', 'http://127.0.0.1:8920', 'prod', 1, 100);

INSERT INTO api_app_channel (app_id, channel_id, priority, enabled)
VALUES (1, 1, 100, 1);

INSERT INTO api_endpoint (id, channel_id, code, name, http_method, path, timeout_ms, retry_max, retry_backoff_ms, circuit_breaker_ratio, enabled)
VALUES (1, 1, 'ping', '连通性探测', 'GET', '/ping', 3000, 1, 100, 0.5, 1);

INSERT INTO api_policy (scope, target_type, target_id, timeout_ms, retry_max, retry_backoff_ms, circuit_breaker_enabled, circuit_breaker_ratio, enabled)
VALUES ('ENDPOINT', 'ENDPOINT', 1, 2000, 1, 80, 1, 0.4, 1);

INSERT INTO api_auth_config (channel_id, auth_type, token_cache_ttl_seconds, enabled)
VALUES (1, 'NONE', 300, 1);

INSERT INTO api_config_version (config_type, target_type, target_id, version, status, gray_percent, config_json, published_by, published_at)
VALUES ('APP', 'APP', 1, 1, 'PUBLISHED', 100, '{}', 'seed', CURRENT_TIMESTAMP);
