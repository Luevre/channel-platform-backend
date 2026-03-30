# 渠道投放管理平台 - 后端服务

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.3 | 主框架 |
| Java | 17 | 运行环境 |
| MyBatis-Plus | 3.5.6 | ORM，自动分页/逻辑删除 |
| MySQL | 8.0+ | 主数据库 |
| Spring Security | 6.x | 安全框架 |
| JJWT | 0.12.5 | JWT 鉴权 |
| SpringDoc OpenAPI | 2.3.0 | Swagger 文档自动生成 |
| Lombok | latest | 简化代码 |

---

## 快速启动

### 1. 初始化数据库

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 2. 配置环境变量（或直接修改 application.yml）

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
export JWT_SECRET=your-super-secret-key-change-in-production
```

### 3. 启动服务

```bash
mvn spring-boot:run
```

服务启动在 `http://localhost:8080/api`

### 4. 访问 Swagger 文档

```
http://localhost:8080/api/swagger-ui.html
```

---

## API 接口总览

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 用户登录，返回 JWT Token |
| POST | /api/auth/logout | 退出登录（客户端清除 Token） |

### 回传节点管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/channel/conv-nodes | 分页查询 |
| GET | /api/channel/conv-nodes/{id} | 查询单个 |
| POST | /api/channel/conv-nodes | 新增 |
| PUT | /api/channel/conv-nodes/{id} | 编辑 |
| DELETE | /api/channel/conv-nodes/{id} | 删除 |
| PUT | /api/channel/conv-nodes/{id}/archive | 归档 |

### 投放参数管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/channel/placement-params | 分页查询 |
| GET | /api/channel/placement-params/{id} | 查询单个 |
| POST | /api/channel/placement-params | 新增 |
| PUT | /api/channel/placement-params/{id} | 编辑 |
| DELETE | /api/channel/placement-params/{id} | 删除 |

---

## 目录结构

```
src/main/java/com/sinolink/channel/
├── ChannelPlatformApplication.java   # 启动类
├── config/
│   ├── AppConfig.java                # MyBatis-Plus分页 + Swagger配置
│   ├── MybatisMetaObjectHandler.java # 自动填充 createdAt/updatedAt
│   └── SecurityConfig.java          # Spring Security + CORS 配置
├── security/
│   ├── JwtAuthenticationFilter.java  # JWT 过滤器
│   └── UserDetailsServiceImpl.java   # 从数据库加载用户
├── util/
│   └── JwtUtil.java                  # JWT 工具类
├── common/
│   ├── result/
│   │   ├── Result.java               # 统一响应包装
│   │   ├── ResultCode.java           # 响应码枚举
│   │   └── PageResult.java           # 分页结果
│   └── exception/
│       ├── BusinessException.java    # 业务异常
│       └── GlobalExceptionHandler.java # 全局异常处理
├── entity/                           # 数据库实体
├── mapper/                           # MyBatis-Plus Mapper
├── dto/
│   ├── request/                      # 入参 DTO
│   └── response/                     # 出参 DTO
├── service/                          # Service 接口
│   └── impl/                         # Service 实现
└── controller/                       # REST Controller
```

---

## 待扩展模块（按前端路由对应）

用 Codex 继续生成以下模块时，参照现有的 ConvNode 模式即可：

- [ ] `PlacementLink` — 投放链接管理（entity/mapper/service/controller）
- [ ] `Deeplink` — DeepLink管理
- [ ] `ConvStrategy` — 回传策略管理
- [ ] `PublicDomainData` — 公域数据管理
- [ ] `SysUser` — 用户管理（修改密码、角色管理）

---

## 前端对接说明

### 登录获取 Token

```javascript
// POST /api/auth/login
const res = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123' })
});
const { data: { token } } = await res.json();
localStorage.setItem('token', token);
```

### 带 Token 请求（Axios 拦截器）

```javascript
// 在 axios 实例中添加请求拦截器
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器处理 401
axios.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

---

## Codex 提示词模板

后续用 Codex 生成新模块时，可使用如下 Prompt：

```
参照项目中已有的 ConvNode 模块（entity/mapper/service/controller），
为【投放链接】功能生成完整的后端代码。
表名: placement_link，字段参考 schema.sql。
保持相同的包结构、注解风格、异常处理和分页方式。
```
