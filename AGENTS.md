# AGENTS.md

面向 Java 企业应用的第三方接口动态注册与管理框架（third-api）。本文件为 AI 编码代理提供项目上下文与开发约束。

## 项目定位

让短信、支付、鉴权、消息推送等第三方接口以「配置 + 声明式接口」的方式接入：业务代码用注解声明第三方 HTTP 契约，运行时由框架统一完成动态配置、鉴权、重试熔断、调用日志与健康监测，支持配置热更新，无需重启业务服务。

## 技术栈与约束（重要）

- **JDK 8**（`maven.compiler.source/target = 1.8`，所有代码必须兼容 Java 8，禁止使用 Java 9+ API）
- **Maven 3.6+**，Spring Boot **2.7.18**（非 3.x，注意 `javax.*` 而非 `jakarta.*`）
- 语言：Java，代码注释与文档以中文为主
- 本地联调默认 SQLite，生产可切换 MySQL 8
- 版本号统一为 `0.1.0-SNAPSHOT`，由父 POM 统一管理

## 模块结构

| 模块 | 作用 | 关键路径 |
|---|---|---|
| `third-api-sdk-core` | 对外注解与公共模型（被业务项目依赖） | `com.thirdapi.sdk.core.annotation` / `.model` |
| `third-api-spring-boot-starter` | 业务系统接入的 Spring Boot Starter（运行时核心） | `com.thirdapi.starter.*` |
| `third-api-admin-server` | 管理端后端 | `com.thirdapi.admin.*` |
| `third-api-admin-ui/prototype` | 管理端页面原型（纯前端，非 Maven 模块） | — |
| `docs/` | `ddl/schema.sql`（MySQL 模型）、`config-protocol.md`（配置下发协议） | — |
| `scripts/e2e-sqlite.sh` | SQLite 端到端联调脚本 | — |

模块依赖关系：`sdk-core` ← `starter`，`admin-server` 独立。父 POM `pom.xml` 的 `<modules>` 只包含前三个模块。

## 核心概念

- **provider / channel / endpoint**：服务商 / 渠道 / 具体接口，三层定位一个第三方接口
- **声明式接口**：业务侧用 `@ThirdPartyApi` + `@ApiMethod` + `@ApiParam` 描述 HTTP 契约，通过 `ThirdApiClientFactory.create(接口.class)` 获得动态代理
- **配置来源两种模式**：
  - `local`：YAML 中直接配置 `third-api.endpoints[]`
  - `admin`：Starter 通过 HTTP 长轮询从管理端拉取配置快照，发布后热更新（协议见 `docs/config-protocol.md`，未变化返回 `304 Not Modified`）
- **鉴权四类**：`API_KEY`、`BASIC`、`OAUTH2`、`SIGN`（见 `AuthType`、`AuthProcessor`、`ApiSigner`）
- **治理能力**：超时、重试（`max-attempts` / `backoff-ms`）、熔断（阈值 / 最小调用数 / 打开超时，见 `ResiliencePolicy`）

## 常用命令

```bash
# 完整构建 + 单元测试
mvn clean install

# 启动管理端（默认 SQLite，首次自动建表 + 种子数据）
mvn -pl third-api-admin-server spring-boot:run
# 浏览器访问 http://localhost:8080

# 使用 MySQL profile（先执行 docs/ddl/schema.sql）
mvn -pl third-api-admin-server spring-boot:run -Dspring-boot.run.profiles=mysql

# SQLite 端到端联调（建库 → 种子数据 → 启动管理端 → 验证 Starter 拉取配置并调用真实 HTTP 接口）
bash scripts/e2e-sqlite.sh
# 看到 BUILD SUCCESS 即整条链路可用

# 发布到 Maven 仓库（GitHub Packages 默认；Nexus/私服见 README「切换到 Nexus / 私服」）
mvn -Prelease clean deploy
```

## 开发约定

- **新增功能必须同步补充测试**（JUnit 单测；端到端链路测试在 `third-api-spring-boot-starter/src/test/.../e2e/EndToEndTest.java`）
- 提交前运行 `mvn clean install` 确认构建通过
- 修改管理端下发协议时，必须同步更新 `docs/config-protocol.md`
- 修改 MySQL 模型时，必须同步更新 `docs/ddl/schema.sql`
- 管理端本地库使用 SQLite（`*.db` / `*.sqlite` 已在 `.gitignore` 中），不要提交数据库文件
- 业务侧接入 API（注解、`ApiResult`、工厂类）属于对外契约，变更需谨慎并同步更新 README

## CI 与发布（.github/workflows/maven.yml）

- push 到 `main` 或 PR：执行 `mvn clean verify`
- push 到 `main`、push `v*` 标签或手动触发：发布 `third-api-sdk-core` 和 `third-api-spring-boot-starter`（父 POM 一并发布）
- 默认仓库为 GitHub Packages（`https://maven.pkg.github.com/wangzaogen/third-api`，用 `GITHUB_TOKEN`，无需额外 Secret）
- 配置 `MAVEN_REPO_ID` / `MAVEN_REPO_URL` / `MAVEN_SNAPSHOT_URL` / `MAVEN_USERNAME` / `MAVEN_PASSWORD` 这些 Secret 可切换到 Nexus / 私服

## 文档索引

- 完整使用文档：`README.md`（快速开始、业务接入、配置项、管理端功能）
- 配置下发协议：`docs/config-protocol.md`
- 数据模型：`docs/ddl/schema.sql`
