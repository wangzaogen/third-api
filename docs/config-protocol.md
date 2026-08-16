# 管理端配置下发协议

业务系统在 `mode=admin` 时，通过 HTTP 轮询从管理端拉取接口配置快照。

## 拉取接口

```text
GET /api/v1/apps/{appId}/configs?version={version}&longPoll={seconds}
```

请求头：

| Header | 说明 |
|---|---|
| `X-App-Id` | 业务应用 ID |
| `X-App-Secret` | 业务应用密钥 |
| `Accept` | `application/json` |

`version` 为业务侧当前配置版本，`longPoll` 为长轮询等待秒数。

## 响应

配置未变化时返回 `304 Not Modified`，业务侧保持本地快照。

配置有变化时返回 `200 OK`：

```json
{
  "version": 42,
  "configs": [
    {
      "provider": "sms",
      "channel": "aliyun-sms",
      "endpoint": "send",
      "baseUrl": "https://dysmsapi.aliyuncs.com",
      "path": "/v1/sms/send",
      "httpMethod": "POST",
      "enabled": true,
      "timeoutMs": 3000,
      "maxRetries": 2,
      "retryBackoffMs": 200,
      "authType": "OAUTH2",
      "tokenUrl": "https://example.com/oauth/token",
      "clientId": "demo",
      "clientSecret": "demo-secret",
      "apiKey": "",
      "circuitBreakerThreshold": 50,
      "circuitBreakerMinCalls": 5,
      "circuitBreakerOpenTimeoutMs": 10000
    }
  ]
}
```

业务侧收到新版本后更新本地缓存，后续调用立即使用新配置，无需重启。
