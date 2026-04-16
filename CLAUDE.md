# 渠道投放管理平台 - 后端

## 项目概述
这是一个基于 Spring Boot 的渠道投放管理平台后端服务，提供 RESTful API。

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.3 |
| Java版本 | Java 17 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8.x |
| 认证 | JWT (jjwt 0.12.5) |
| 安全 | Spring Security |
| API文档 | SpringDoc OpenAPI 2.3.0 |
| 工具库 | Hutool 5.8.26 |
| 代码生成 | Lombok 1.18.32 |

## 项目结构

```
src/main/java/com/sinolink/channel/
├── ChannelPlatformApplication.java    # 启动类
├── common/
│   ├── exception/                     # 异常处理
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   └── result/                        # 统一响应
│       ├── Result.java
│       ├── ResultCode.java
│       └── PageResult.java
├── config/                             # 配置类
│   ├── AppConfig.java
│   ├── MybatisMetaObjectHandler.java
│   └── SecurityConfig.java
├── controller/                         # 控制器
│   ├── AuthController.java            # 认证接口
│   ├── ConvNodeController.java        # 回传节点
│   ├── ConvStrategyController.java    # 回传策略
│   ├── DeliveryLinkController.java    # 投放链接
│   ├── MyParameterController.java     # 我的参数
│   └── PlacementParamController.java  # 投放参数
├── dto/
│   ├── request/                       # 请求 DTO
│   │   ├── LoginRequest.java
│   │   ├── ConvNodeRequest.java
│   │   ├── ConvNodeQueryRequest.java
│   │   ├── ConvStrategyRequest.java
│   │   ├── ConvStrategyQueryRequest.java
│   │   ├── DeliveryLinkRequest.java
│   │   ├── DeliveryLinkQueryRequest.java
│   │   ├── MyParameterRequest.java
│   │   ├── MyParameterQueryRequest.java
│   │   ├── MyParameterAuditRequest.java
│   │   ├── PlacementParamRequest.java
│   │   ├── PlacementParamQueryRequest.java
│   │   └── PlacementParamAuditRequest.java
│   └── response/                      # 响应 DTO
│       ├── LoginResponse.java
│       ├── ConvStrategyVO.java
│       ├── DeliveryLinkVO.java
│       └── DeliveryPlatformVO.java
├── entity/                            # 实体类
│   ├── ConvNode.java                   # 回传节点
│   ├── ConvStrategy.java               # 回传策略
│   ├── ConvStrategyNode.java           # 策略节点关联
│   ├── DeliveryContent.java           # 投放内容
│   ├── DeliveryLink.java              # 投放链接
│   ├── DeliveryPlatform.java          # 投放平台
│   ├── DeliveryPlatformPosition.java  # 平台广告位
│   ├── MyParameter.java               # 我的参数
│   ├── PlacementParam.java            # 投放参数
│   └── SysUser.java                   # 系统用户
├── mapper/                            # MyBatis Mapper
│   ├── ConvNodeMapper.java
│   ├── ConvStrategyMapper.java
│   ├── ConvStrategyNodeMapper.java
│   ├── DeliveryContentMapper.java
│   ├── DeliveryLinkMapper.java
│   ├── DeliveryPlatformMapper.java
│   ├── DeliveryPlatformPositionMapper.java
│   ├── MyParameterMapper.java
│   ├── PlacementParamMapper.java
│   └── SysUserMapper.java
├── security/                           # 安全相关
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
├── service/                           # 服务层
│   ├── AuthService.java
│   ├── ConvNodeService.java
│   ├── ConvStrategyService.java
│   ├── DeliveryLinkService.java
│   ├── MyParameterService.java
│   └── PlacementParamService.java
└── util/                              # 工具类
    └── JwtUtil.java                   # JWT 工具
```

## 核心模块说明

### 认证模块
- `AuthController` - 提供登录接口，返回 JWT Token
- `JwtAuthenticationFilter` - 请求拦截验证 JWT
- `JwtUtil` - JWT 生成与解析

### 回传节点管理
- `ConvNodeController` - CRUD 操作，支持归档功能

### 回传策略管理
- `ConvStrategyController` - 策略的增删改查

### 投放参数管理
- `PlacementParamController` - 投放参数管理

### 我的参数管理
- `MyParameterController` - 用户的自定义参数

### 投放链接管理
- `DeliveryLinkController` - 投放链接管理

## 数据库表结构

| 表名 | 说明 |
|------|------|
| sys_user | 系统用户表 |
| conv_node | 回传节点表 |
| conv_strategy | 回传策略表 |
| placement_param | 投放参数表 |
| placement_link | 投放链接表 |
| deeplink | DeepLink管理表 |
| public_domain_data | 公域数据表 |

## 开发偏好

1. **代码交付方式**：提供完整的文件供替换，而非分步编辑
2. **确认机制**：修改前分步确认，确保方向正确后再继续
3. **API风格**：遵循 RESTful 规范，统一响应格式 Result<T>
4. **认证方式**：使用 JWT Token，Header 中传递 Authorization: Bearer xxx

## 接口文档
启动服务后访问: http://localhost:8080/swagger-ui.html

## 启动配置
- 默认端口: 8080
- 数据库: MySQL (需在 application.properties 中配置)