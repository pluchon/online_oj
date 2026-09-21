# 墨衡 OJ 后端升级计划书

> 拟定日期：2026-09-21
> 版本号均于当日从 Maven Central 核对；执行前如有新补丁版本，只在同一小版本内跟进。

## 路线总览

```mermaid
flowchart LR
    P0["阶段 0<br/>代码优化"] --> P1["阶段 1<br/>框架升级 Boot 3.5.16"]
    P1 --> P2["阶段 2<br/>链路追踪"]
    P2 --> P3["阶段 3<br/>AI 模块 oj_ai"]
    P3 --> P4["阶段 4<br/>Sentinel 熔断限流"]
```

| 阶段 | 目标 | 前置条件 |
|---|---|---|
| 0 代码优化 | 在旧版本上清理代码，得到干净基线 | 无 |
| 1 框架升级 | 全量升到 Spring Boot 3.5.16 | 阶段 0 提交完成，打标签 `pre-upgrade` |
| 2 链路追踪 | 网关、Feign、RabbitMQ 全链路可追踪 | 阶段 1 验收通过 |
| 3 AI 模块 | 新增 `oj_ai`，接入 Spring AI | 阶段 1 验收通过 |
| 4 Sentinel | 仅在 `JudgeClient`、`AiClient` 两个同步调用上熔断 | 阶段 3 上线，AI 调用真实存在 |

每个阶段单独提交、单独验收，不跨阶段混改。

---

## 阶段 0：代码优化

在当前 Boot 3.0.1 上进行，范围随讨论逐项确定，不预设清单。原则：

- 只做重构，不改业务行为、接口路径和返回结构。
- 每一批改动都用 IDEA 自带 Maven 编译通过后再进入下一批。
- 顺手可做的构建清理：根 `pom.xml` 的 `<properties>` 里有重复声明（`mybatis-plus.version`、`springdoc-openapi.version`、`fastjson.version`、`jwt.version`、`hutool-all.version` 等），合并为一份。

完成后提交，并打标签 `pre-upgrade`，作为升级失败时的回退点。

---

## 阶段 1：框架升级

### 1.1 目标版本矩阵

| 组件 | 当前 | 目标 | 说明 |
|---|---|---|---|
| Spring Boot | 3.0.1 | **3.5.16** | 3.5 线最后一个正式版 |
| Spring Cloud | 2022.0.0 | **2025.0.3** | 2025.0.x 对应 Boot 3.5 |
| Spring Cloud Alibaba | 2022.0.0.0-RC2 | **2025.0.0.0** | 对应 Spring Cloud 2025.0；内置 Nacos 客户端 3.0.3、Sentinel 1.8.9 |
| Spring AI | 无 | **1.1.8**（阶段 3 引入） | 1.1.x 基于 Boot 3.5 构建；2.x 面向 Boot 4，不采用 |
| MyBatis-Plus | 3.5.5 | **3.5.17** | 继续用 `mybatis-plus-spring-boot3-starter` |
| PageHelper Starter | 2.0.0 | **2.1.1** | 2.x 为 Boot 3 线；4.x 不采用 |
| springdoc-openapi | 2.2.0 | **2.8.17** | 2.8.x 对应 Boot 3.5；3.x 面向 Boot 4 |
| xxl-job-core | 2.4.0 | 2.4.0（不动） | 需与 xxl-job-admin 2.4.0 镜像配对，单独升级没有收益 |
| jjwt | 0.9.1 | 0.9.1（不动） | 见 1.8 |
| Java | 17 | 17 | Boot 3.5 最低 17，无需变更 |

### 1.2 bootstrap.yml 迁移到 spring.config.import

SCA 2025.x 起 Nacos 配置走 `spring.config.import`，不再依赖 bootstrap 上下文。

- 涉及 5 个模块：`oj_gateway`、`oj_friend`、`oj_job`、`oj_judge`、`oj_system`。
- 删除根 pom 中的 `spring-cloud-starter-bootstrap` 依赖。
- 各模块 `bootstrap.yml` 改为 `application.yml`，只保留：应用名、端口、profile、Nacos 地址与命名空间（环境变量引用）、`spring.config.import`。
- `shared-configs`（如 `oj-message-local.yaml`）改写为多条 `optional:nacos:` / `nacos:` 导入；生产必需配置不加 `optional:`。
- 验收：Data ID 覆盖顺序与迁移前一致；Nacos 不可用时启动行为符合预期（必需配置缺失应启动失败，而不是带着默认值跑起来）。

### 1.3 Nacos 客户端与服务端版本

SCA 2025.0.0.0 自带 Nacos 客户端 3.0.3，本地服务端是 `nacos-server:v2.2.3`。

| 方案 | 做法 | 风险 |
|---|---|---|
| A（推荐） | 服务端升级到 Nacos 3.0.x，与客户端同代 | 3.x 控制台默认占用 8080，与 xxl-job-admin 冲突，需改端口映射；需配置鉴权相关环境变量；配置数据需从旧库迁移并核对 |
| B | 在依赖管理中把 `nacos-client` 固定为 2.5.x，服务端不动 | 偏离 SCA 官方测试组合，后续排错成本高 |

执行前确认选 A 还是 B。选 A 时先导出旧命名空间配置做备份。

### 1.4 Spring Cloud Gateway 变更

Spring Cloud 2025.0 中网关模块改名：

- 依赖 `spring-cloud-starter-gateway` → `spring-cloud-starter-gateway-server-webflux`。
- 配置前缀 `spring.cloud.gateway.*` → `spring.cloud.gateway.server.webflux.*`（Nacos 中的网关路由与白名单配置一并迁移）。
- 验收：所有路由可达；白名单接口无需令牌；受保护接口无令牌返回 401。

### 1.5 持久层与文档

- MyBatis-Plus 3.5.17：项目分页使用 PageHelper，未使用 `PaginationInnerInterceptor`，不需要额外引入 jsqlparser 模块。升级后回归所有 Lambda 查询、`@TableLogic` 逻辑删除和自动填充。
- PageHelper 2.1.1：回归提交记录、题目列表、竞赛列表、排名的分页与 total。
- springdoc 2.8.17：确认各服务文档页可打开。

### 1.6 Elasticsearch

Boot 3.5 对应的 ES Java 客户端为 8.18 系列，本地服务端为 8.5.3，IK 分词插件版本也锁定在 8.5.3。

- 先在旧服务端上验证题目搜索是否正常；正常则服务端暂不升级。
- 如需升级服务端，ES、Kibana 与 IK 插件三者版本必须完全一致，并重建题目索引。

### 1.7 RabbitMQ、Redis、XXL-JOB

- 客户端随 Boot 升级即可，服务端镜像不动。
- 回归：提交 → 判题 → 结果回写全链路；竞赛定时任务正常执行。

### 1.8 暂不升级 jjwt

jjwt 0.12 的 API 与密钥长度校验都有变化，升级等同于改动认证链路，可能导致已签发令牌全部失效。本轮不动，单独立项。

### 1.9 阶段 1 验收

- [ ] IDEA 自带 Maven 全量编译通过
- [ ] 5 个服务全部启动并注册到 Nacos 正确命名空间
- [ ] 登录（B 端、C 端）、受保护接口、令牌过期处理正常
- [ ] 题目运行、提交、判题结果、提交记录分页正常
- [ ] 竞赛报名、竞赛提交校验、赛后排名正常
- [ ] 题目搜索正常
- [ ] 前端两端 `npm run build` 通过，主流程在浏览器走通

---

## 阶段 2：链路追踪

### 2.1 选型

**Micrometer Tracing + Brave + Zipkin。** Boot 3 已移除 Sleuth；Zipkin 只需一个容器。SkyWalking 需要 OAP、UI 与存储，对本项目偏重，不采用。

### 2.2 覆盖范围

| 链路段 | 做法 |
|---|---|
| 网关 | 响应式链路，开启 `spring.reactor.context-propagation=auto` |
| friend → judge（Feign 同步运行） | 引入 `feign-micrometer`，自动传播 trace 头 |
| friend → MQ → judge → MQ → friend（异步提交） | 开启 RabbitTemplate 与监听容器的 observation，trace 穿过消息队列 |
| 日志 | 日志格式输出 traceId / spanId，便于按一次请求检索各服务日志 |

### 2.3 配置与部署

- 依赖：`micrometer-tracing-bridge-brave`、`zipkin-reporter-brave`，放在需要追踪的服务模块中，不放进 `oj_common_core`。
- 采样率、Zipkin 地址写在 Nacos 公共配置中；本地采样率 1.0，生产按需下调。
- `deploy/docker-compose.yml` 新增 `zipkin` 服务。

### 2.4 验收

- [ ] 一次"提交"请求在 Zipkin 中呈现为一条完整 trace：网关 → friend → MQ → judge → MQ → friend
- [ ] 一次"运行"请求呈现网关 → friend → Feign → judge
- [ ] 各服务日志中的 traceId 与 Zipkin 一致

---

## 阶段 3：AI 模块

### 3.1 模块位置

```
oj_api      api/ai/   AiInternalApi（/ai/internal/**）与 DTO/VO，只放契约
oj_modules/oj_ai      Controller / Service / Prompt / 模型配置，AI 服务本体
oj_system   client/   AiFeignClient + AiClient，B 端调用方
oj_friend   client/   AiFeignClient + AiClient，C 端调用方
```

- 前端不直接调用 `oj_ai`，统一经 friend / system 转发。
- 权限、额度、提交归属等判断由 friend / system 负责；`oj_ai` 只做计算，不写任何业务表。
- `oj_ai` 不依赖 friend / system，也不直接读业务库；所需上下文由调用方放在请求 DTO 中传入。

### 3.2 技术选型

- Spring AI 1.1.8，经 `spring-ai-bom` 统一版本。
- 模型提供方待定（见"待确认事项"）。优先使用 OpenAI 兼容接口，方便后续切换。
- API Key 只从环境变量或 Nacos 加密配置读取，不写入源码和日志。

### 3.3 功能顺序

| 顺序 | 功能 | 调用方 | 说明 |
|---|---|---|---|
| 1 | 生成测试用例 | system（B 端） | 输入题面与标程，生成 `tb_question_case` 的展示格式与判题格式；生成结果先预览，由管理员确认后再入库 |
| 2 | 错题提示 | friend（C 端） | 提交未通过后，发送代码与失败用例，只给思路不给完整代码；friend 校验提交归属与每日次数 |
| 3 | 解释编译错误 | friend（C 端） | 把编译信息转述为易懂的中文 |

B 端题目管理接入 `tb_question_case`（当前尚未完成）是功能 1 的前置工作，放在本阶段开头完成。

### 3.4 失败语义

- `AiClient` 调用失败返回明确错误码（如"AI 服务繁忙"），前端给出提示，不影响原有判题流程。
- 超时时间单独配置，不沿用 Feign 默认值。

### 3.5 验收

- [ ] B 端可生成并确认入库一组测试用例，入库后可正常运行、提交
- [ ] C 端错题提示受次数限制，非本人提交无法调用
- [ ] 模型服务不可用时，页面给出明确提示，其余功能正常

---

## 阶段 4：Sentinel

### 4.1 适用范围

只加在两个跨服务同步调用的调用方边界上：

| 调用 | 位置 | 理由 |
|---|---|---|
| friend → judge 运行 | `oj_friend` 的 `JudgeClient` | 判题需启动容器，耗时不稳定，会占住 friend 线程 |
| friend / system → oj_ai | 各自的 `AiClient` | 模型接口慢且不稳定 |

不做全局默认规则，不做网关层限流，不部署带持久化的 Sentinel 控制台。

### 4.2 做法

- 使用 SCA 2025.0.0.0 自带的 Sentinel 1.8.9。
- 在 `JudgeClient` / `AiClient` 上配置资源，熔断降级返回明确错误码，不吞异常、不伪造成功。
- 阈值放 Nacos 数据源，不写死在代码中。

### 4.3 验收

- [ ] 每条规则附复现步骤：如人为让 judge 超时，连续触发后 friend 快速返回"判题繁忙"，恢复后自动放行
- [ ] 熔断期间其他接口不受影响

---

## 待确认事项

| 编号 | 事项 | 影响阶段 |
|---|---|---|
| Q1 | Nacos 采用方案 A（服务端升级到 3.0.x）还是方案 B（固定 2.5.x 客户端） | 阶段 1 |
| Q2 | ES 服务端是否随之升级（取决于 8.5.3 服务端能否与新客户端正常工作） | 阶段 1 |
| Q3 | 大模型提供方，以及是否需要流式输出 | 阶段 3 |
| Q4 | 是否需要记录 AI 调用日志表（用于额度统计与审计） | 阶段 3 |

## 暂缓事项（2026-09-21 代码优化中发现）

| 编号 | 事项 | 现状 | 建议 |
|---|---|---|---|
| T1 | 竞赛结算无触发方 | **已完成（2026-09-21）**：`tb_exam.rank_settled` 标记 + friend 内部接口 `/friend/internal/exam/rank/settle`，job 任务 `examRankSettlementHandler` 触发；查看排名不再写库 | 需在 XXL-JOB 控制台登记并启用该任务 |
| T2 | 竞赛缓存跨服务耦合 | **已完成（2026-09-21）**：缓存归 friend 独有，system 变更竞赛/竞赛题目后与 job 定时任务调用 `/friend/internal/exam/cache/refresh` | — |
| T4 | 判题沙箱共享挂载 | **已完成（2026-09-21）**：容器不再挂载宿主机目录，每次评测 `docker cp` 进借用容器的私有目录，结束后结束残留进程并删除目录，清理失败即淘汰容器 | 判题服务重启时会自动清理旧的带挂载容器 |
| T3 | Nacos 网关白名单拼写 | `oj-gateway-local.yaml` 中 `/**/webjars/**m` 多了结尾的 m | 在 Nacos 控制台改为 `/**/webjars/**` |

## 回退方案

- 阶段 1 开始前打标签 `pre-upgrade`；升级分支验收不通过则整体回退到该标签。
- Nacos 服务端升级前导出全部命名空间配置；MySQL 升级前备份 `bitoj_dev` 与 Nacos 配置库。
- 不轮换 JWT 密钥、数据库与 MQ 密码。
