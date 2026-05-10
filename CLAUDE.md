# 渠道投放管理平台 — 后端

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.2.3 |
| Java | JDK | 17 |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.x |
| 认证 | Spring Security + JWT (jjwt) | 0.12.5 |
| API 文档 | SpringDoc OpenAPI | 2.3.0 |
| 工具库 | Hutool | 5.8.26 |
| 代码生成 | Lombok | 1.18.32 |
| AI | Anthropic Claude API (RestTemplate) | — |

## 运行

```bash
mvn spring-boot:run   # http://localhost:8080
# Swagger UI: http://localhost:8080/api/swagger-ui.html
```

## 项目路径

- 后端根目录：`F:\国金证券\05WEEK_04\后端\channel-platform-backend`
- 前端根目录：`D:\360data\重要数据\我的文档\New project`
- Java 包基路径：`com.sinolink.channel`

## 目录结构（完整）

```
src/main/java/com/sinolink/channel/
├── ChannelPlatformApplication.java         # 启动类 (@MapperScan)
├── common/
│   ├── exception/
│   │   ├── BusinessException.java          # 业务异常
│   │   └── GlobalExceptionHandler.java     # 全局异常处理
│   └── result/
│       ├── Result.java                     # 统一响应 {code, message, data}
│       ├── ResultCode.java                 # 响应码枚举
│       └── PageResult.java                 # 分页响应 {records, total, current, size}
├── config/
│   ├── AppConfig.java                      # RestTemplate + MyBatis-Plus 分页 + OpenAPI
│   ├── MybatisMetaObjectHandler.java       # 自动填充 createdAt / updatedAt
│   └── SecurityConfig.java                 # Spring Security 配置 + CORS + 白名单
├── controller/
│   ├── AgentController.java                # AI 助手 POST /agent/chat + GET /agent/ping
│   ├── AuthController.java                 # 登录 POST /auth/login
│   ├── ConvNodeController.java             # 回传节点 /channel/conv-nodes
│   ├── ConvStrategyController.java         # 回传策略 /channel/conv-strategies
│   ├── DeliveryLinkController.java         # 投放链接 /delivery/links + 内容/平台/位置
│   ├── MyConvNodeController.java           # 我的回传节点 /channel/my-conv-nodes
│   ├── MyDeliverySceneController.java      # 我的投放场景 /delivery/my-scenes
│   ├── MyParameterController.java          # 我的参数 /channel/my-params
│   └── PlacementParamController.java       # 投放参数 /channel/placement-params
├── dto/
│   ├── request/
│   │   ├── AgentChatRequest.java           # AI 对话请求 {messages[], sessionId}
│   │   ├── LoginRequest.java
│   │   ├── ConvNodeRequest.java
│   │   ├── ConvNodeQueryRequest.java
│   │   ├── ConvStrategyRequest.java
│   │   ├── ConvStrategyQueryRequest.java
│   │   ├── DeliveryLinkRequest.java
│   │   ├── DeliveryLinkQueryRequest.java
│   │   ├── MyConvNodeRequest.java
│   │   ├── MyConvNodeQueryRequest.java
│   │   ├── MyConvNodeAuditRequest.java
│   │   ├── MyDeliverySceneRequest.java
│   │   ├── MyDeliverySceneQueryRequest.java
│   │   ├── MyDeliverySceneAuditRequest.java
│   │   ├── MyParameterRequest.java
│   │   ├── MyParameterQueryRequest.java
│   │   ├── MyParameterAuditRequest.java
│   │   ├── PlacementParamRequest.java
│   │   ├── PlacementParamQueryRequest.java
│   │   └── PlacementParamAuditRequest.java
│   └── response/
│       ├── AgentChatResponse.java          # AI 对话响应 {content, sessionId}
│       ├── LoginResponse.java
│       ├── ConvStrategyVO.java
│       ├── DeliveryLinkVO.java
│       └── DeliveryPlatformVO.java
├── entity/
│   ├── PlacementParam.java                 # 投放参数（含审核字段）
│   ├── MyParameter.java                    # 我的参数（审核流：草稿→提交→归档）
│   ├── ConvNode.java                       # 回传节点
│   ├── ConvStrategy.java                   # 回传策略
│   ├── ConvStrategyNode.java               # 策略-节点关联表
│   ├── DeliveryLink.java                   # 投放链接
│   ├── DeliveryContent.java                # 投放内容
│   ├── DeliveryPlatform.java               # 投放平台
│   ├── DeliveryPlatformPosition.java       # 平台位置
│   ├── MyConvNode.java                     # 我的回传节点
│   ├── MyDeliveryScene.java                # 我的投放场景
│   ├── ParamDict.java                      # 参数字典
│   └── SysUser.java                        # 系统用户
├── mapper/                                 # 全部继承 BaseMapper<T>
│   ├── PlacementParamMapper.java
│   ├── MyParameterMapper.java
│   ├── ConvNodeMapper.java
│   ├── ConvStrategyMapper.java
│   ├── ConvStrategyNodeMapper.java
│   ├── DeliveryLinkMapper.java
│   ├── DeliveryContentMapper.java
│   ├── DeliveryPlatformMapper.java
│   ├── DeliveryPlatformPositionMapper.java
│   ├── MyConvNodeMapper.java
│   ├── MyDeliverySceneMapper.java
│   ├── ParamDictMapper.java
│   └── SysUserMapper.java
├── security/
│   ├── JwtAuthenticationFilter.java        # JWT 过滤器（无 token 直接放行）
│   └── UserDetailsServiceImpl.java         # 从 sys_user 加载用户
├── service/
│   ├── AgentService.java                   # AI 助手接口
│   ├── AuthService.java
│   ├── ConvNodeService.java
│   ├── ConvStrategyService.java
│   ├── DeliveryLinkService.java
│   ├── MyConvNodeService.java
│   ├── MyDeliverySceneService.java
│   ├── MyParameterService.java
│   ├── ParamDictService.java
│   ├── PlacementParamService.java
│   └── impl/
│       ├── AgentServiceImpl.java           # Claude API 调用 + Function Calling 循环
│       ├── AuthServiceImpl.java
│       ├── ConvNodeServiceImpl.java
│       ├── ConvStrategyServiceImpl.java
│       ├── DeliveryLinkServiceImpl.java
│       ├── MyConvNodeServiceImpl.java
│       ├── MyDeliverySceneServiceImpl.java
│       ├── MyParameterServiceImpl.java
│       ├── ParamDictServiceImpl.java
│       └── PlacementParamServiceImpl.java
└── util/
    └── JwtUtil.java                        # JWT 生成 / 解析 / 验证

src/main/resources/
├── application.yml                         # 主配置（端口/数据库/JWT/Anthropic/Swagger）
├── schema.sql                              # 建表 + 初始数据
└── logback-spring.xml                      # 日志配置
```

## 数据库表

数据库名：`channel_platform`

| 表名 | 说明 | 审核流 |
|------|------|--------|
| `sys_user` | 系统用户（ADMIN/OPERATOR/VIEWER） | — |
| `placement_param` | 投放参数（参数总表） | auditStatus |
| `my_parameter` | 我的参数申请 | status + auditResult |
| `param_dict` | 参数字典（关联 placement_param） | — |
| `conv_node` | 回传节点 | — |
| `conv_strategy` | 回传策略 | — |
| `conv_strategy_node` | 策略-节点关联 | — |
| `delivery_link` | 投放链接 | — |
| `delivery_content` | 投放内容 | — |
| `delivery_platform` | 投放平台 | — |
| `delivery_platform_position` | 平台位置 | — |
| `my_conv_node` | 我的回传节点申请 | status + auditResult |
| `my_delivery_scene` | 我的投放场景申请 | status + auditResult |
| `deeplink` | DeepLink（未接入前端） | — |
| `public_domain_data` | 公域数据（未接入前端） | — |

### 审核流状态值

```
status:      0=草稿  1=已提交  2=已归档
auditResult: 0=待审核  1=已通过  2=已驳回
```

### 默认用户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | ADMIN |
| 吴小青 | admin123 | OPERATOR |

## 开发注意事项

### context-path
- `server.servlet.context-path: /api`
- 所有 Controller 路径 **不带** `/api` 前缀
- SecurityConfig 白名单也 **不带** `/api` 前缀
- 外部请求完整路径示例：`POST http://localhost:8080/api/agent/chat`

### Spring Security 白名单

```java
"/auth/login", "/auth/register", "/agent/**",
"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health"
```

新增公开接口时需要在 `SecurityConfig.WHITE_LIST` 中添加路径。

### Claude API 代理
- 代理地址：`https://api.aipaibox.com`（CC Switch 服务）
- 配置在 `application.yml` 的 `anthropic.base-url`
- API Key 在 `anthropic.api-key`
- 调用方式：`RestTemplate` 直接 POST 到 `{base-url}/v1/messages`
- 不使用 Anthropic Java SDK

### AI 助手 Function Calling（AgentServiceImpl）
- Agent 循环：调用 Claude → 检查 stop_reason → 若 tool_use 则执行 → 回传 tool_result → 再调用
- 最多 5 轮工具调用
- 已实现 3 个 Tools：
  - `query_params` — 查 placement_param 表（模糊搜索 + 分类筛选）
  - `get_param_detail` — 查 placement_param 单条详情（按 id 或 paramKey）
  - `create_param_application` — 写入 my_parameter 表（status=1 已提交, auditResult=0 待审核）

### CORS
- 允许 `http://localhost:*` 和 `https://*.sinolink.com`
- 前端 dev server 在 `http://localhost:5173`

### 逻辑删除
- 全局配置 `deleted` 字段（0=正常 1=删除）
- MyBatis-Plus 自动过滤

### 统一响应格式

```json
{ "code": 200, "message": "操作成功", "data": { ... } }
```

分页响应 data 为：
```json
{ "records": [...], "total": 100, "current": 1, "size": 10 }
```
