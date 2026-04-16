# 渠道投放管理平台 — 后端服务

> 国金证券渠道投放管理平台的后端 RESTful API 服务，基于 **Spring Boot 3.2 + MyBatis-Plus + MySQL + Spring Security + JWT** 构建。

---

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.2.3 | 主框架 |
| Spring Security | 6.x | 安全认证框架 |
| MyBatis-Plus | 3.5.7 | ORM，支持自动分页 & 逻辑删除 |
| MySQL | 8.0+ | 关系型数据库 |
| JJWT | 0.12.5 | JWT 令牌签发与验证 |
| SpringDoc OpenAPI | 2.3.0 | Swagger API 文档自动生成 |
| Hutool | 5.8.26 | Java 工具库 |
| Lombok | 1.18.32 | 消除样板代码 |

---

## 已实现模块

| 模块 | Controller | 说明 |
|------|-----------|------|
| 用户认证 | `AuthController` | 登录 / 退出，JWT Token 签发 |
| 投放参数管理 | `PlacementParamController` | CRUD + 审核 |
| 我的参数管理 | `MyParameterController` | CRUD + 提交审核 |
| 回传节点管理 | `ConvNodeController` | CRUD + 归档 |
| 回传策略管理 | `ConvStrategyController` | CRUD，关联回传节点 |
| 投放链接管理 | `DeliveryLinkController` | CRUD |

---

## 快速启动

### 环境要求

- **JDK** >= 17
- **Maven** >= 3.8
- **MySQL** >= 8.0

### 1. 初始化数据库

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 2. 配置环境变量（或直接修改 `application.yml`）

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
export JWT_SECRET=your-super-secret-key-change-in-production
```

### 3. 启动服务

```bash
mvn spring-boot:run
```

服务地址：`http://localhost:8080/api`

### 4. 访问 Swagger 文档

```
http://localhost:8080/api/swagger-ui.html
```

---

## API 接口总览

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录，返回 JWT Token |
| POST | `/api/auth/logout` | 退出登录（客户端清除 Token） |

### 投放参数管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/channel/placement-params` | 分页查询 |
| GET | `/api/channel/placement-params/{id}` | 查询单个 |
| POST | `/api/channel/placement-params` | 新增 |
| PUT | `/api/channel/placement-params/{id}` | 编辑 |
| DELETE | `/api/channel/placement-params/{id}` | 删除 |

### 回传节点管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/channel/conv-nodes` | 分页查询 |
| GET | `/api/channel/conv-nodes/{id}` | 查询单个 |
| POST | `/api/channel/conv-nodes` | 新增 |
| PUT | `/api/channel/conv-nodes/{id}` | 编辑 |
| DELETE | `/api/channel/conv-nodes/{id}` | 删除 |
| PUT | `/api/channel/conv-nodes/{id}/archive` | 归档 |

### 回传策略管理 / 投放链接管理 / 我的参数管理

接口风格与上述一致，详见 Swagger 文档。

---

## 项目结构

```
src/main/java/com/sinolink/channel/
├── ChannelPlatformApplication.java      # 启动类
├── config/
│   ├── AppConfig.java                   # MyBatis-Plus 分页 + Swagger 配置
│   ├── MybatisMetaObjectHandler.java    # 自动填充 createdAt / updatedAt
│   └── SecurityConfig.java             # Spring Security + CORS
├── security/
│   ├── JwtAuthenticationFilter.java     # JWT 过滤器
│   └── UserDetailsServiceImpl.java      # 从 DB 加载用户
├── util/
│   └── JwtUtil.java                     # JWT 工具类
├── common/
│   ├── result/                          # 统一响应包装 (Result / PageResult / ResultCode)
│   └── exception/                       # 全局异常处理
├── entity/                              # 数据库实体 (7 张表)
├── mapper/                              # MyBatis-Plus Mapper (10 个)
├── dto/
│   ├── request/                         # 请求 DTO
│   └── response/                        # 响应 VO
├── service/                             # Service 接口
│   └── impl/                            # Service 实现 (6 个)
└── controller/                          # REST Controller (6 个)

src/main/resources/
├── application.yml                      # 应用配置
├── schema.sql                           # 数据库初始化脚本
└── logback-spring.xml                   # 日志配置
```

---

## 核心设计

- **统一响应格式**：所有接口返回 `Result<T>` / `PageResult<T>` 结构
- **JWT 鉴权**：Spring Security 过滤链 + 自定义 `JwtAuthenticationFilter`
- **逻辑删除**：MyBatis-Plus 全局配置 `deleted` 字段
- **自动填充**：`createdAt` / `updatedAt` 由 `MybatisMetaObjectHandler` 自动注入
- **全局异常**：`GlobalExceptionHandler` 统一捕获业务异常与参数校验异常
- **CORS**：`SecurityConfig` 中开放跨域支持前端开发联调

---

## 前端对接

```javascript
// Axios 请求拦截器：自动附加 Token
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

---

## 待扩展

- [ ] DeepLink 管理
- [ ] 公域数据管理
- [ ] 渠道投放成效报表
- [ ] 用户 & 角色管理（修改密码、权限控制）
