# Third API Admin Server

管理端后端，负责接口配置的查询、发布，以及向业务 starter 提供配置拉取接口。

## 运行

默认使用 SQLite 本地库，首次启动会自动建表和写入种子数据：

```bash
bash scripts/e2e-sqlite.sh
```

也可以直接启动管理端：

```bash
mvn -pl third-api-admin-server spring-boot:run
```

生产 MySQL 环境使用 `mysql` profile：

```bash
mvn -pl third-api-admin-server spring-boot:run -Dspring-boot.run.profiles=mysql
```

默认监听 `8080`。

启动后浏览器访问 `http://localhost:8080/` 即进入管理页面。

管理页面包含：应用、服务商、渠道、接口、鉴权配置、健康监测、审计日志。

## 接口

- `GET /api/v1/apps/{appId}/configs?version={version}&longPoll={seconds}`
  业务 starter 配置拉取接口，配置未变化返回 `304`
- `POST /api/v1/apps/{appId}/publish?operator=admin`
  基于当前启用的渠道和接口配置生成新版本并发布
