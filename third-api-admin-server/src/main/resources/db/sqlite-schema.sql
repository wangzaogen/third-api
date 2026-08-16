DROP TABLE IF EXISTS api_app_channel;
DROP TABLE IF EXISTS api_health_check;
DROP TABLE IF EXISTS api_audit_log;
DROP TABLE IF EXISTS api_call_log;
DROP TABLE IF EXISTS api_config_version;
DROP TABLE IF EXISTS api_auth_config;
DROP TABLE IF EXISTS api_policy;
DROP TABLE IF EXISTS api_endpoint;
DROP TABLE IF EXISTS api_channel;
DROP TABLE IF EXISTS api_provider;
DROP TABLE IF EXISTS api_app;

CREATE TABLE api_app (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    app_id TEXT NOT NULL,
    app_name TEXT NOT NULL,
    app_secret_encrypted TEXT,
    enabled INTEGER NOT NULL DEFAULT 1,
    remark TEXT,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (app_id)
);

CREATE TABLE api_provider (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'OTHER',
    enabled INTEGER NOT NULL DEFAULT 1,
    remark TEXT,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (code)
);

CREATE TABLE api_channel (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    provider_id INTEGER NOT NULL,
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    base_url TEXT NOT NULL,
    environment TEXT NOT NULL DEFAULT 'prod',
    enabled INTEGER NOT NULL DEFAULT 1,
    status TEXT NOT NULL DEFAULT 'ONLINE',
    priority INTEGER NOT NULL DEFAULT 100,
    health_status TEXT NOT NULL DEFAULT 'UNKNOWN',
    last_health_at TEXT,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (code),
    FOREIGN KEY (provider_id) REFERENCES api_provider (id)
);

CREATE TABLE api_app_channel (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    app_id INTEGER NOT NULL,
    channel_id INTEGER NOT NULL,
    priority INTEGER NOT NULL DEFAULT 100,
    enabled INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (app_id, channel_id),
    FOREIGN KEY (app_id) REFERENCES api_app (id),
    FOREIGN KEY (channel_id) REFERENCES api_channel (id)
);

CREATE TABLE api_endpoint (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_id INTEGER NOT NULL,
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    http_method TEXT NOT NULL DEFAULT 'POST',
    path TEXT NOT NULL,
    content_type TEXT NOT NULL DEFAULT 'application/json',
    timeout_ms INTEGER NOT NULL DEFAULT 5000,
    retry_max INTEGER NOT NULL DEFAULT 2,
    retry_backoff_ms INTEGER NOT NULL DEFAULT 200,
    circuit_breaker_ratio REAL NOT NULL DEFAULT 0.50,
    enabled INTEGER NOT NULL DEFAULT 1,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (channel_id, code),
    FOREIGN KEY (channel_id) REFERENCES api_channel (id)
);

CREATE TABLE api_policy (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    scope TEXT NOT NULL DEFAULT 'ENDPOINT',
    target_type TEXT NOT NULL DEFAULT 'ENDPOINT',
    target_id INTEGER NOT NULL,
    timeout_ms INTEGER,
    retry_max INTEGER,
    retry_backoff_ms INTEGER,
    circuit_breaker_enabled INTEGER NOT NULL DEFAULT 1,
    circuit_breaker_ratio REAL,
    rate_limit_per_second INTEGER,
    bulkhead_max_concurrent INTEGER,
    enabled INTEGER NOT NULL DEFAULT 1,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (scope, target_type, target_id)
);

CREATE TABLE api_auth_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_id INTEGER NOT NULL,
    auth_type TEXT NOT NULL DEFAULT 'NONE',
    token_url TEXT,
    client_id TEXT,
    client_secret_encrypted TEXT,
    extra_config TEXT,
    token_cache_ttl_seconds INTEGER NOT NULL DEFAULT 300,
    enabled INTEGER NOT NULL DEFAULT 1,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (channel_id),
    FOREIGN KEY (channel_id) REFERENCES api_channel (id)
);

CREATE TABLE api_config_version (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_type TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_id INTEGER NOT NULL,
    version INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'DRAFT',
    gray_percent INTEGER NOT NULL DEFAULT 0,
    config_json TEXT NOT NULL,
    published_by TEXT,
    published_at TEXT,
    rolled_back_at TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (config_type, target_type, target_id, version)
);

CREATE TABLE api_call_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    trace_id TEXT NOT NULL,
    app_name TEXT NOT NULL,
    provider_code TEXT NOT NULL,
    channel_code TEXT NOT NULL,
    endpoint_code TEXT NOT NULL,
    http_method TEXT NOT NULL,
    url TEXT NOT NULL,
    request_body TEXT,
    response_body TEXT,
    http_status INTEGER,
    biz_code TEXT,
    success INTEGER NOT NULL DEFAULT 1,
    cost_ms INTEGER NOT NULL DEFAULT 0,
    error_type TEXT,
    error_message TEXT,
    request_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE api_health_check (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_id INTEGER NOT NULL,
    endpoint_id INTEGER,
    check_type TEXT NOT NULL DEFAULT 'HTTP',
    target_url TEXT,
    success INTEGER NOT NULL DEFAULT 0,
    status_code INTEGER,
    cost_ms INTEGER,
    error_message TEXT,
    checked_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (channel_id) REFERENCES api_channel (id)
);

CREATE TABLE api_audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operator TEXT NOT NULL,
    action TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_id INTEGER NOT NULL,
    before_json TEXT,
    after_json TEXT,
    client_ip TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
