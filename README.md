# 非物质文化遗产展示与 AI 问答系统 - 项目文档

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 项目架构](#2-项目架构)
- [3. 技术栈选型](#3-技术栈选型)
- [4. 功能模块划分](#4-功能模块划分)
- [5. 数据库设计](#5-数据库设计)
- [6. API 接口规范](#6-api-接口规范)
- [7. 前端组件结构](#7-前端组件结构)
- [8. 开发环境配置](#8-开发环境配置)
- [9. 部署流程](#9-部署流程)
- [10. 测试策略](#10-测试策略)
- [11. 维护指南](#11-维护指南)
- [12. 常见问题解决方案](#12-常见问题解决方案)

---

## 1. 项目概述

### 1.1 项目简介

非物质文化遗产展示与 AI 问答系统（Intangible Cultural Heritage Promotion System）是一个集文化遗产展示、信息管理、AI 智能问答于一体的综合性 Web 应用系统。系统旨在通过现代化的技术手段，展示和传播非物质文化遗产，提供便捷的文化遗产信息查询和互动体验。

### 1.2 项目目标

- 建立非物质文化遗产信息数据库
- 提供文化遗产项目的展示和查询功能
- 实现 AI 智能问答，解答用户关于文化遗产的问题
- 提供用户收藏、评论等互动功能
- 支持后台管理和数据统计分析

### 1.3 项目结构

```
BS/
├── demo/                          # 后端项目（Spring Boot）
│   ├── src/main/java/
│   │   └── com/example/demo/
│   │       ├── controller/        # 控制器层
│   │       ├── service/           # 服务层
│   │       ├── entity/            # 实体类
│   │       ├── mapper/            # 数据访问层
│   │       ├── config/            # 配置类
│   │       ├── interceptor/       # 拦截器
│   │       ├── util/              # 工具类
│   │       └── common/            # 公共类
│   ├── src/main/resources/
│   │   ├── application.yml        # 配置文件
│   │   └── mapper/                # MyBatis XML 映射文件
│   ├── pom.xml                    # Maven 配置
│   └── files/                     # 上传文件存储
│
└── ich-frontend/                  # 前端项目（Vue 3 + Vite）
    ├── src/
    │   ├── views/                 # 页面组件
    │   ├── components/            # 公共组件
    │   ├── router/                # 路由配置
    │   ├── utils/                 # 工具函数
    │   └── main.ts                # 入口文件
    ├── package.json               # 依赖配置
    └── vite.config.js             # Vite 配置
```

---

## 2. 项目架构

### 2.1 系统架构图

```
┌─────────────────────────────────────────────────────────┐
│                      用户层                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │  浏览器   │  │  移动端   │  │  管理员   │              │
│  └──────────┘  └──────────┘  └──────────┘              │
└─────────────────────────────────────────────────────────┘
                        ↓ HTTPS
┌─────────────────────────────────────────────────────────┐
│                    前端应用层                            │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Vue 3 + TypeScript + Element Plus               │   │
│  │  - 组件化开发                                     │   │
│  │  - 路由管理（Vue Router）                         │   │
│  │  - 状态管理（Pinia）                              │   │
│  │  - HTTP 客户端（Axios）                           │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                        ↓ RESTful API
┌─────────────────────────────────────────────────────────┐
│                   后端应用层                             │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Spring Boot 2.7.0                               │   │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐ │   │
│  │  │ Controller │→ │  Service   │→ │   Mapper   │ │   │
│  │  └────────────┘  └────────────┘  └────────────┘ │   │
│  │  - JWT 认证           - 业务逻辑      - 数据访问   │   │
│  │  - 全局异常处理       - 事务管理      - MyBatis   │   │
│  │  - 日志记录（AOP）                                │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                        ↓ JDBC
┌─────────────────────────────────────────────────────────┐
│                    数据存储层                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │  MySQL   │  │  Redis   │  │  文件存储 │              │
│  │  主数据库 │  │  缓存    │  │  静态资源 │              │
│  └──────────┘  └──────────┘  └──────────┘              │
└─────────────────────────────────────────────────────────┘
                        ↓ API 调用
┌─────────────────────────────────────────────────────────┐
│                   第三方服务                             │
│  ┌──────────────────┐  ┌──────────────────┐            │
│  │  DeepSeek AI API │  │   WebSocket      │            │
│  │  智能问答服务    │  │   实时通信        │            │
│  └──────────────────┘  └──────────────────┘            │
└─────────────────────────────────────────────────────────┘
```

### 2.2 技术架构特点

- **前后端分离**：前端负责展示和交互，后端提供 API 服务
- **RESTful API**：标准化的接口设计，便于维护和扩展
- **JWT 认证**：无状态的身份认证机制
- **Redis 缓存**：提升系统性能，减轻数据库压力
- **MyBatis-Plus**：简化数据库操作，提高开发效率
- **WebSocket**：支持实时通信功能

---

## 3. 技术栈选型

### 3.1 后端技术栈

| 技术             | 版本   | 说明         |
| ---------------- | ------ | ------------ |
| **Java**         | 1.8    | 开发语言     |
| **Spring Boot**  | 2.7.0  | 核心框架     |
| **MyBatis-Plus** | 3.5.3  | ORM 框架     |
| **MySQL**        | 8.0.28 | 关系型数据库 |
| **Redis**        | -      | 缓存数据库   |
| **Lettuce**      | -      | Redis 客户端 |
| **JWT (jjwt)**   | 0.11.5 | 身份认证     |
| **Lombok**       | -      | 简化代码     |
| **Hutool**       | 5.8.26 | Java 工具库  |
| **EasyExcel**    | 3.3.2  | Excel 处理   |
| **WebSocket**    | -      | 实时通信     |
| **AOP**          | -      | 面向切面编程 |

### 3.2 前端技术栈

| 技术             | 版本   | 说明                   |
| ---------------- | ------ | ---------------------- |
| **Vue**          | 3.5.24 | 渐进式 JavaScript 框架 |
| **TypeScript**   | -      | JavaScript 超集        |
| **Vite**         | 7.2.5  | 下一代前端构建工具     |
| **Vue Router**   | 4.6.4  | 官方路由管理器         |
| **Pinia**        | 3.0.4  | 状态管理库             |
| **Element Plus** | 2.13.1 | UI 组件库              |
| **Axios**        | 1.13.2 | HTTP 客户端            |
| **ECharts**      | 6.0.0  | 数据可视化             |
| **Markdown-it**  | 14.1.0 | Markdown 解析器        |
| **Vue Quill**    | 1.2.0  | 富文本编辑器           |

### 3.3 开发工具

| 工具              | 说明               |
| ----------------- | ------------------ |
| **Maven**         | 项目构建和依赖管理 |
| **Git**           | 版本控制           |
| **VS Code**       | 前端开发 IDE       |
| **IntelliJ IDEA** | 后端开发 IDE       |

---

## 4. 功能模块划分

### 4.1 功能模块总览

```
┌─────────────────────────────────────────────┐
│           非物质文化遗产展示系统              │
├─────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐          │
│  │  用户模块   │  │  认证模块   │          │
│  └─────────────┘  └─────────────┘          │
│  ┌─────────────┐  ┌─────────────┐          │
│  │  项目展示   │  │  传承人管理 │          │
│  └─────────────┘  └─────────────┘          │
│  ┌─────────────┐  ┌─────────────┐          │
│  │  AI 聊天    │  │  收藏评论   │          │
│  └─────────────┘  └─────────────┘          │
│  ┌─────────────┐  ┌─────────────┐          │
│  │  热度排行   │  │  搜索历史   │          │
│  └─────────────┘  └─────────────┘          │
│  ┌─────────────┐  ┌─────────────┐          │
│  │  文件管理   │  │  统计管理   │          │
│  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────┘
```

### 4.2 核心功能模块

#### 4.2.1 用户认证模块

**功能描述**：

- 用户登录、注册
- JWT Token 认证
- 密码重置
- 权限管理

**相关文件**：

- 后端：`LoginController.java`, `AuthController.java`, `JwtInterceptor.java`
- 前端：`Login.vue`

#### 4.2.2 非遗项目管理模块

**功能描述**：

- 项目列表展示（分页、筛选）
- 项目详情查看
- 项目创建、编辑、删除（管理员）
- 项目审核

**相关文件**：

- 后端：`IchProjectController.java`
- 前端：`Home.vue`

#### 4.2.3 传承人管理模块

**功能描述**：

- 传承人列表展示
- 传承人详情查看
- 传承人与项目关联

**相关文件**：

- 后端：`IchInheritorController.java`
- 前端：`Inheritor.vue`

#### 4.2.4 AI 聊天模块

**功能描述**：

- 智能问答对话
- 聊天会话管理
- 批量删除聊天记录
- 历史记录查询

**相关文件**：

- 后端：`ChatController.java`, `ChatSessionService.java`
- 前端：`Chat.vue`

#### 4.2.5 收藏评论模块

**功能描述**：

- 项目收藏
- 收藏列表管理
- 项目评论
- 评论管理

**相关文件**：

- 后端：`AppFavoriteController.java`, `AppCommentController.java`
- 前端：`Favorites.vue`

#### 4.2.6 统计管理模块

**功能描述**：

- 项目数量统计
- 用户活跃度统计
- 热度排行
- 浏览量统计

**相关文件**：

- 后端：`StatisticsController.java`, `ViewCountController.java`
- 前端：`HotRanking.vue`

#### 4.2.7 文件管理模块

**功能描述**：

- 图片上传
- 视频上传
- 文件存储和管理

**相关文件**：

- 后端：`FileController.java`
- 前端：各组件中的文件上传功能

---

## 5. 数据库设计

### 5.1 数据库表总览

#### 核心业务表

| 表名            | 说明       | 主要字段                                                                                         |
| --------------- | ---------- | ------------------------------------------------------------------------------------------------ |
| `ich_project`   | 非遗项目表 | id, name, category_id, region_id, protect_level, status, history, features, cover_url, video_url |
| `ich_inheritor` | 传承人表   | id, name, gender, birth_date, region_code, project_id                                            |
| `ich_category`  | 类别表     | id, name, parent_id, level                                                                       |
| `ich_region`    | 地区表     | id, name, code, parent_id                                                                        |
| `ich_media`     | 媒体资源表 | id, project_id, type, url, description                                                           |

#### 用户相关表

| 表名                  | 说明           | 主要字段                                                                      |
| --------------------- | -------------- | ----------------------------------------------------------------------------- |
| `sys_user`            | 用户表         | id, username, password_hash, nickname, phone, email, avatar_url, role, status |
| `sys_role`            | 角色表         | id, name, code, description                                                   |
| `sys_permission`      | 权限表         | id, name, code, type, path                                                    |
| `sys_user_role`       | 用户角色关联表 | user_id, role_id                                                              |
| `sys_role_permission` | 角色权限关联表 | role_id, permission_id                                                        |

#### 互动功能表

| 表名           | 说明   | 主要字段                                            |
| -------------- | ------ | --------------------------------------------------- |
| `app_favorite` | 收藏表 | id, user_id, project_id, created_at                 |
| `app_comment`  | 评论表 | id, user_id, project_id, content, parent_id, status |

#### AI 聊天表

| 表名              | 说明       | 主要字段                                                                |
| ----------------- | ---------- | ----------------------------------------------------------------------- |
| `chat_session`    | 聊天会话表 | id, user_id, title, last_message, message_count, created_at, updated_at |
| `ai_chat_history` | 聊天记录表 | id, user_id, chat_id, question, answer, created_at                      |

#### 系统功能表

| 表名                | 说明       | 主要字段                                                     |
| ------------------- | ---------- | ------------------------------------------------------------ |
| `sys_operation_log` | 操作日志表 | id, user_id, operation, method, params, time, ip, created_at |
| `ich_project_view`  | 项目浏览表 | id, project_id, user_id, view_time, ip                       |
| `search_history`    | 搜索历史表 | id, user_id, keyword, search_time                            |

### 5.2 核心表结构详情

#### 5.2.1 sys_user（用户表）

```sql
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像 URL',
  `region_code` varchar(20) DEFAULT NULL COMMENT '地区编码',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态：1 启用 0 禁用',
  `role` varchar(50) DEFAULT 'user' COMMENT '角色',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

#### 5.2.2 ich_project（非遗项目表）

```sql
CREATE TABLE `ich_project` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '项目名称',
  `category_id` bigint(20) DEFAULT NULL COMMENT '类别 ID',
  `region_id` bigint(20) DEFAULT NULL COMMENT '地区 ID',
  `protect_level` varchar(50) DEFAULT NULL COMMENT '保护级别',
  `status` varchar(50) DEFAULT NULL COMMENT '存续状态',
  `history` text COMMENT '历史渊源',
  `features` text COMMENT '核心特征',
  `cover_url` varchar(255) DEFAULT NULL COMMENT '封面图',
  `video_url` varchar(255) DEFAULT NULL COMMENT '视频 URL',
  `longitude` decimal(10,8) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,8) DEFAULT NULL COMMENT '纬度',
  `address` varchar(255) DEFAULT NULL COMMENT '详细地址',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `opening_hours` varchar(100) DEFAULT NULL COMMENT '开放时间',
  `audit_status` tinyint(4) DEFAULT '0' COMMENT '审核状态：0 待审核 1 通过 2 拒绝',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_region` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='非遗项目表';
```

#### 5.2.3 chat_session（聊天会话表）

```sql
CREATE TABLE `chat_session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `title` varchar(200) DEFAULT '新会话' COMMENT '会话标题',
  `last_message` text COMMENT '最后一条消息',
  `message_count` int(11) DEFAULT '0' COMMENT '消息数量',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';
```

#### 5.2.4 ai_chat_history（聊天记录表）

```sql
CREATE TABLE `ai_chat_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `chat_id` bigint(20) NOT NULL COMMENT '会话 ID',
  `question` text NOT NULL COMMENT '用户问题',
  `answer` text NOT NULL COMMENT 'AI 回答',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_chat` (`user_id`, `chat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 聊天记录表';
```

### 5.3 数据库索引设计

**性能优化索引**：

1. **用户相关索引**

   - `sys_user.uk_username`: 用户名唯一索引
   - `sys_user.idx_phone`: 手机号索引
   - `sys_user.idx_email`: 邮箱索引

2. **项目相关索引**

   - `ich_project.idx_category`: 类别 ID 索引
   - `ich_project.idx_region`: 地区 ID 索引
   - `ich_project.idx_audit_status`: 审核状态索引

3. **聊天相关索引**

   - `chat_session.idx_user_id`: 用户 ID 索引
   - `ai_chat_history.idx_user_chat`: 用户 ID + 会话 ID 联合索引

4. **互动功能索引**
   - `app_favorite.idx_user_project`: 用户 ID + 项目 ID 联合索引
   - `app_comment.idx_project`: 项目 ID 索引

---

## 6. API 接口规范

### 6.1 接口设计规范

#### 6.1.1 RESTful 风格

- **资源命名**：使用名词复数形式，如 `/api/projects`
- **HTTP 方法**：
  - `GET`：查询资源
  - `POST`：创建资源
  - `PUT`：更新资源（全量）
  - `PATCH`：更新资源（部分）
  - `DELETE`：删除资源

#### 6.1.2 统一响应格式

```json
{
  "code": 200,
  "msg": "success",
  "data": {},
  "timestamp": 1234567890
}
```

**响应码说明**：

| 响应码 | 说明               |
| ------ | ------------------ |
| 200    | 成功               |
| 400    | 请求参数错误       |
| 401    | 未授权，需要登录   |
| 403    | 禁止访问，权限不足 |
| 404    | 资源不存在         |
| 500    | 服务器内部错误     |

#### 6.1.3 认证机制

使用 JWT Bearer Token 认证：

```
Authorization: Bearer <token>
```

### 6.2 核心接口文档

#### 6.2.1 认证接口

**1. 用户登录**

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}

Response:
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员",
      "role": "admin"
    }
  }
}
```

**2. 用户注册**

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "string",
  "password": "string",
  "nickname": "string",
  "email": "string"
}
```

**3. 重置密码**

```http
POST /api/auth/reset-password
Content-Type: application/json

{
  "username": "string",
  "email": "string"
}
```

#### 6.2.2 项目管理接口

**1. 获取项目列表**

```http
GET /api/projects?page=1&limit=10&categoryId=1&regionId=2
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "data": {
    "list": [...],
    "total": 100,
    "page": 1,
    "limit": 10
  }
}
```

**2. 获取项目详情**

```http
GET /api/projects/{id}
Authorization: Bearer <token>
```

**3. 创建项目**

```http
POST /api/projects
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "string",
  "categoryId": 1,
  "regionId": 1,
  "history": "string",
  "features": "string"
}
```

#### 6.2.3 AI 聊天接口

**1. 发送消息**

```http
POST /api/chat/send
Content-Type: application/json
Authorization: Bearer <token>

{
  "message": "string",
  "userId": 1,
  "chatId": 123,  // 可选，不传则创建新会话
  "title": "新会话"  // 可选，仅在新会话时有效
}

Response:
{
  "code": 200,
  "data": {
    "reply": "AI 回复内容",
    "chatId": 123,
    "sessionTitle": "会话标题"
  }
}
```

**2. 获取会话列表**

```http
GET /api/chat/sessions?userId=1&page=1&limit=10
Authorization: Bearer <token>
```

**3. 删除会话**

```http
DELETE /api/chat/sessions/{id}?userId=1
Authorization: Bearer <token>
```

**4. 获取聊天记录**

```http
GET /api/chat/history?userId=1&chatId=123&limit=20
Authorization: Bearer <token>
```

#### 6.2.4 收藏接口

**1. 收藏项目**

```http
POST /api/favorites
Content-Type: application/json
Authorization: Bearer <token>

{
  "projectId": 1
}
```

**2. 获取收藏列表**

```http
GET /api/favorites?userId=1&page=1&limit=10
Authorization: Bearer <token>
```

**3. 取消收藏**

```http
DELETE /api/favorites/{id}
Authorization: Bearer <token>
```

#### 6.2.5 评论接口

**1. 发表评论**

```http
POST /api/comments
Content-Type: application/json
Authorization: Bearer <token>

{
  "projectId": 1,
  "content": "string",
  "parentId": null  // 回复评论时填写父评论 ID
}
```

**2. 获取评论列表**

```http
GET /api/comments?projectId=1&page=1&limit=20
```

#### 6.2.6 文件上传接口

**1. 上传图片**

```http
POST /api/files/upload/image
Content-Type: multipart/form-data
Authorization: Bearer <token>

file: <image_file>

Response:
{
  "code": 200,
  "data": {
    "url": "http://localhost:8080/files/xxx.jpg"
  }
}
```

**2. 上传视频**

```http
POST /api/files/upload/video
Content-Type: multipart/form-data
Authorization: Bearer <token>

file: <video_file>
```

#### 6.2.7 统计接口

**1. 获取统计数据**

```http
GET /api/statistics/overview
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "data": {
    "projectCount": 100,
    "inheritorCount": 50,
    "userCount": 200,
    "viewCount": 10000
  }
}
```

**2. 获取热度排行**

```http
GET /api/statistics/hot-ranking?limit=10
```

---

## 7. 前端组件结构

### 7.1 组件树结构

```
App.vue
└── RouterView
    ├── Login.vue (登录页)
    └── Layout.vue (布局页)
        ├── Sidebar (侧边栏)
        ├── Header (顶部栏)
        └── RouterView
            ├── Home.vue (首页 - 项目列表)
            ├── Inheritor.vue (传承人列表)
            ├── User.vue (用户管理)
            ├── Profile.vue (个人中心)
            ├── Favorites.vue (我的收藏)
            ├── HotRanking.vue (热度排行)
            └── Chat.vue (AI 聊天)
```

### 7.2 核心组件说明

#### 7.2.1 Layout.vue（主布局组件）

**功能**：

- 提供系统主框架（侧边栏 + 顶栏 + 内容区）
- 导航菜单管理
- 用户信息显示

**关键代码**：

```vue
<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header>
        <Header />
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
```

#### 7.2.2 Login.vue（登录组件）

**功能**：

- 用户登录表单
- 表单验证
- Token 存储
- 登录成功跳转

**关键逻辑**：

```typescript
const handleLogin = async () => {
  const res = await request.post("/auth/login", {
    username: form.value.username,
    password: form.value.password,
  });

  // 保存 token 和用户信息
  sessionStorage.setItem("token", res.data.data.token);
  sessionStorage.setItem("user", JSON.stringify(res.data.data.user));

  // 跳转到首页
  router.push("/home");
};
```

#### 7.2.3 Home.vue（项目列表组件）

**功能**：

- 项目列表展示（分页）
- 项目筛选（类别、地区）
- 项目搜索
- 项目详情查看

**关键特性**：

- 使用 Element Plus Table 组件
- 支持图片预览
- 支持视频播放
- 收藏功能集成

#### 7.2.4 Chat.vue（AI 聊天组件）

**功能**：

- 聊天会话列表
- 消息发送和接收
- 聊天记录展示
- 批量删除聊天记录
- Markdown 渲染

**关键特性**：

- 实时消息显示
- 自动滚动到底部
- 支持多会话管理
- 批量操作模式

**批量删除功能**：

```typescript
const batchDelete = async () => {
  if (selectedChats.value.length === 0) {
    ElMessage.warning("请选择要删除的聊天记录");
    return;
  }

  await ElMessageBox.confirm(
    `确认删除选中的 ${selectedChats.value.length} 条聊天记录吗？`,
    "警告",
    { type: "warning" }
  );

  // 批量删除
  const deletePromises = selectedChats.value.map((chatId) =>
    request.delete(`/chat/sessions/${chatId}`, {
      params: { userId },
    })
  );

  await Promise.all(deletePromises);
};
```

#### 7.2.5 Favorites.vue（收藏组件）

**功能**：

- 收藏列表展示
- 取消收藏
- 跳转到项目详情

#### 7.2.6 HotRanking.vue（热度排行组件）

**功能**：

- 项目热度排行
- 浏览量统计展示
- ECharts 图表展示

### 7.3 公共组件

#### 7.3.1 ChatAssistant.vue（聊天助手组件）

**功能**：

- 全局聊天助手入口
- 悬浮按钮
- 快速打开聊天窗口

### 7.4 工具模块

#### 7.4.1 request.ts（HTTP 请求封装）

**功能**：

- Axios 实例创建
- 请求拦截器（添加 Token）
- 响应拦截器（统一错误处理）

**关键代码**：

```typescript
const service = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 10000,
});

// 请求拦截器
service.interceptors.request.use((config) => {
  const userStr = sessionStorage.getItem("user");
  if (userStr) {
    const user = JSON.parse(userStr);
    config.headers["Authorization"] = `Bearer ${user.token}`;
  }
  return config;
});

// 响应拦截器
service.interceptors.response.use((response) => {
  const res = response.data;
  if (res.code === 200) {
    return response;
  } else {
    ElMessage.error(res.msg || "操作失败");
    return Promise.reject(new Error(res.msg));
  }
});
```

#### 7.4.2 errorHandler.ts（错误处理模块）

**功能**：

- 统一错误处理
- 错误提示
- 401 跳转登录

---

## 8. 开发环境配置

### 8.1 环境要求

#### 后端环境

| 软件  | 版本 | 说明          |
| ----- | ---- | ------------- |
| JDK   | 1.8+ | Java 开发环境 |
| Maven | 3.6+ | 项目构建工具  |
| MySQL | 8.0+ | 数据库        |
| Redis | 6.0+ | 缓存服务      |

#### 前端环境

| 软件    | 版本 | 说明                |
| ------- | ---- | ------------------- |
| Node.js | 16+  | JavaScript 运行环境 |
| npm     | 8+   | 包管理器            |
| VS Code | -    | 推荐编辑器          |

### 8.2 后端环境配置

#### 8.2.1 JDK 安装

1. 下载 JDK 1.8 或更高版本
2. 配置环境变量：
   ```bash
   JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202
   PATH=%JAVA_HOME%\bin;%PATH%
   ```
3. 验证安装：
   ```bash
   java -version
   ```

#### 8.2.2 Maven 配置

1. 下载 Maven 并解压
2. 配置环境变量：
   ```bash
   MAVEN_HOME=D:\apache-maven-3.6.3
   PATH=%MAVEN_HOME%\bin;%PATH%
   ```
3. 配置阿里云镜像（settings.xml）：
   ```xml
   <mirror>
     <id>aliyun</id>
     <mirrorOf>central</mirrorOf>
     <name>Aliyun Maven</name>
     <url>https://maven.aliyun.com/repository/public</url>
   </mirror>
   ```

#### 8.2.3 MySQL 配置

1. 安装 MySQL 8.0
2. 创建数据库：
   ```sql
   CREATE DATABASE ich_promotion_db
   DEFAULT CHARACTER SET utf8mb4
   COLLATE utf8mb4_unicode_ci;
   ```
3. 创建用户并授权：
   ```sql
   CREATE USER 'ich_user'@'localhost' IDENTIFIED BY 'password';
   GRANT ALL PRIVILEGES ON ich_promotion_db.* TO 'ich_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

#### 8.2.4 Redis 配置

1. Windows 下载 Redis
2. 启动 Redis 服务：
   ```bash
   redis-server.exe
   ```
3. 验证连接：
   ```bash
   redis-cli ping
   # 应返回 PONG
   ```

#### 8.2.5 配置文件修改

修改 `application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ich_promotion_db?serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379

deepseek:
  api:
    key: your_deepseek_api_key
```

#### 8.2.6 启动后端项目

```bash
cd d:\BS\demo
mvn clean install
mvn spring-boot:run
```

访问：http://localhost:8080

### 8.3 前端环境配置

#### 8.3.1 Node.js 安装

1. 下载 Node.js 16+ 版本
2. 安装后验证：
   ```bash
   node -v
   npm -v
   ```

#### 8.3.2 安装依赖

```bash
cd d:\BS\ich-frontend
npm install
```

#### 8.3.3 配置开发环境

修改 `vite.config.js`（如需要）：

```javascript
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
```

#### 8.3.4 启动前端项目

```bash
# 开发模式
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

访问：http://localhost:3000

### 8.4 开发工具配置

#### 8.4.1 VS Code 推荐插件

- Volar（Vue 开发）
- ESLint
- Prettier
- Auto Close Tag
- Auto Rename Tag
- Path Intellisense

#### 8.4.2 IntelliJ IDEA 配置

- 安装 Lombok 插件
- 配置 Maven
- 配置数据库连接

---

## 9. 部署流程

### 9.1 生产环境要求

| 组件   | 要求                            | 说明              |
| ------ | ------------------------------- | ----------------- |
| 服务器 | Linux (CentOS 7+/Ubuntu 18.04+) | 推荐 4 核 8G 以上 |
| JDK    | 1.8+                            | Java 运行环境     |
| Nginx  | 1.18+                           | Web 服务器        |
| MySQL  | 8.0+                            | 数据库            |
| Redis  | 6.0+                            | 缓存              |

### 9.2 后端部署

#### 9.2.1 打包项目

```bash
cd d:\BS\demo
mvn clean package -DskipTests
```

生成文件：`target/demo-0.0.1-SNAPSHOT.jar`

#### 9.2.2 上传到服务器

```bash
scp target/demo-0.0.1-SNAPSHOT.jar user@server:/opt/ich-system/
```

#### 9.2.3 配置生产环境

创建 `application-prod.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ich_promotion_db?useSSL=true
    username: prod_user
    password: prod_password
  redis:
    host: localhost
    port: 6379

deepseek:
  api:
    key: prod_api_key
```

#### 9.2.4 启动服务

```bash
# 后台启动
nohup java -jar demo-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=8080 \
  > app.log 2>&1 &

# 查看日志
tail -f app.log
```

#### 9.2.5 配置 systemd 服务

创建 `/etc/systemd/system/ich-system.service`：

```ini
[Unit]
Description=ICH Promotion System
After=syslog.target network.target

[Service]
User=root
WorkingDirectory=/opt/ich-system
ExecStart=/usr/bin/java -jar /opt/ich-system/demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
ExecStop=/bin/kill -15 $MAINPID
Restart=always

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
systemctl daemon-reload
systemctl start ich-system
systemctl enable ich-system
systemctl status ich-system
```

### 9.3 前端部署

#### 9.3.1 构建生产版本

```bash
cd d:\BS\ich-frontend
npm run build
```

生成文件：`dist/` 目录

#### 9.3.2 配置 Nginx

创建 `/etc/nginx/conf.d/ich-system.conf`：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /var/www/ich-frontend;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 文件资源
    location /files/ {
        proxy_pass http://localhost:8080/files/;
        proxy_set_header Host $host;
    }
}
```

#### 9.3.3 部署前端文件

```bash
# 上传 dist 目录
scp -r dist/* user@server:/var/www/ich-frontend/

# 重启 Nginx
nginx -t
systemctl restart nginx
```

### 9.4 数据库迁移

#### 9.4.1 导出数据库结构

```bash
mysqldump -u root -p --no-data ich_promotion_db > schema.sql
```

#### 9.4.2 导入到生产环境

```bash
mysql -u prod_user -p ich_promotion_db < schema.sql
```

### 9.5 部署验证

#### 9.5.1 健康检查

```bash
# 检查后端服务
curl http://localhost:8080/api/health

# 检查前端
curl http://localhost/

# 检查数据库连接
mysql -u prod_user -p -e "SELECT 1"
```

#### 9.5.2 功能测试

1. 访问前端页面
2. 测试登录功能
3. 测试项目列表
4. 测试 AI 聊天
5. 测试文件上传

---

## 10. 测试策略

### 10.1 测试类型

#### 10.1.1 单元测试

**后端单元测试**：

```java
@SpringBootTest
class ChatControllerTest {

    @Autowired
    private ChatController chatController;

    @Test
    void testSend() {
        Map<String, Object> params = new HashMap<>();
        params.put("message", "测试消息");
        params.put("userId", 1L);

        Result<Map<String, Object>> result = chatController.send(params);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData().get("reply"));
    }
}
```

**运行测试**：

```bash
mvn test
```

#### 10.1.2 前端组件测试

使用 Vitest 进行组件测试：

```typescript
import { mount } from "@vue/test-utils";
import { describe, it, expect } from "vitest";
import Chat from "../src/views/Chat.vue";

describe("Chat.vue", () => {
  it("renders properly", () => {
    const wrapper = mount(Chat, {
      global: {
        plugins: [router],
      },
    });
    expect(wrapper.text()).toContain("AI 聊天");
  });
});
```

### 10.2 集成测试

#### 10.2.1 API 接口测试

使用 Postman 或 JMeter 进行接口测试：

**测试用例**：

1. 登录接口测试
2. 项目列表接口测试
3. 聊天接口测试
4. 文件上传接口测试

### 10.3 性能测试

#### 10.3.1 压力测试

使用 JMeter 进行压力测试：

- 并发用户数：100, 500, 1000
- 测试接口：`/api/projects`, `/api/chat/send`
- 监控指标：响应时间、吞吐量、错误率

#### 10.3.2 数据库性能测试

```sql
-- 查询执行计划
EXPLAIN SELECT * FROM ich_project WHERE category_id = 1;

-- 慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;
```

### 10.4 测试报告

测试完成后生成测试报告，包括：

- 测试覆盖率
- 通过率
- 性能指标
- 问题清单

---

## 11. 维护指南

### 11.1 日志管理

#### 11.1.1 日志配置

`application.yml`：

```yaml
logging:
  level:
    root: INFO
    com.example.demo: DEBUG
  file:
    name: logs/app.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

#### 11.1.2 日志分析

```bash
# 查看错误日志
grep ERROR logs/app.log

# 查看最近 100 行
tail -100 logs/app.log

# 实时查看日志
tail -f logs/app.log
```

### 11.2 数据库维护

#### 11.2.1 定期备份

```bash
#!/bin/bash
# backup.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/opt/backups"

mysqldump -u root -p ich_promotion_db > ${BACKUP_DIR}/ich_${DATE}.sql

# 删除 30 天前的备份
find ${BACKUP_DIR} -name "ich_*.sql" -mtime +30 -delete
```

#### 11.2.2 数据库优化

```sql
-- 分析表
ANALYZE TABLE ich_project;

-- 优化表
OPTIMIZE TABLE ich_project;

-- 检查索引使用情况
SHOW INDEX FROM ich_project;
```

### 11.3 性能监控

#### 11.3.1 系统监控

```bash
# CPU 使用率
top

# 内存使用率
free -h

# 磁盘使用率
df -h

# 网络流量
iftop
```

#### 11.3.2 应用监控

- JVM 内存监控
- 线程池监控
- 数据库连接池监控
- Redis 缓存命中率

### 11.4 版本更新

#### 11.4.1 更新流程

1. 备份当前版本
2. 停止服务
3. 更新代码
4. 执行数据库迁移
5. 启动服务
6. 验证功能

#### 11.4.2 回滚方案

```bash
# 备份当前版本
cp demo.jar demo.jar.backup

# 回滚
mv demo.jar.backup demo.jar
systemctl restart ich-system
```

---

## 12. 常见问题解决方案

### 12.1 认证问题

#### 问题 1：401 未授权错误

**现象**：接口返回 401 错误

**原因**：

- Token 过期
- Token 格式错误
- 未携带 Token

**解决方案**：

```typescript
// 前端检查 token
const userStr = sessionStorage.getItem("user");
if (!userStr) {
  // 跳转到登录页
  router.push("/login");
  return;
}

const user = JSON.parse(userStr);
if (!user.token) {
  router.push("/login");
  return;
}
```

#### 问题 2：Token 过期处理

**解决方案**：

```typescript
// request.ts 中添加响应拦截器
service.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token 过期，清除本地存储
      sessionStorage.removeItem("user");
      sessionStorage.removeItem("token");

      // 跳转到登录页
      router.push("/login");
    }
    return Promise.reject(error);
  }
);
```

### 12.2 数据库问题

#### 问题 1：数据库连接失败

**现象**：启动时报数据库连接错误

**解决方案**：

1. 检查 MySQL 服务状态：

   ```bash
   systemctl status mysql
   ```

2. 检查数据库配置：

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/ich_promotion_db
       username: root
       password: correct_password
   ```

3. 检查数据库是否存在：
   ```sql
   SHOW DATABASES LIKE 'ich_promotion_db';
   ```

#### 问题 2：慢查询优化

**解决方案**：

1. 启用慢查询日志
2. 分析慢查询 SQL
3. 添加索引优化
4. 优化 SQL 语句

### 12.3 前端问题

#### 问题 1：跨域问题

**现象**：前端请求后端报 CORS 错误

**解决方案**：

后端 `WebConfig.java`：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

#### 问题 2：静态资源加载失败

**现象**：页面刷新后 404

**解决方案**：

Nginx 配置：

```nginx
location / {
    root /var/www/ich-frontend;
    index index.html;
    try_files $uri $uri/ /index.html;
}
```

### 12.4 AI 聊天问题

#### 问题 1：DeepSeek API 调用失败

**现象**：聊天接口返回错误

**解决方案**：

1. 检查 API Key 配置：

   ```yaml
   deepseek:
     api:
       key: sk-xxxxxxxxxxxx
   ```

2. 检查网络连接：

   ```bash
   curl https://api.deepseek.com
   ```

3. 增加超时时间：
   ```java
   // 在调用 API 时增加超时设置
   HttpRequest.post(url)
     .timeout(60000)
     .execute();
   ```

#### 问题 2：聊天记录不显示

**解决方案**：

1. 检查数据库连接
2. 检查 chatId 参数是否正确传递
3. 检查前端数据渲染逻辑

### 12.5 文件上传问题

#### 问题 1：上传失败

**现象**：文件上传返回错误

**解决方案**：

1. 检查文件大小限制：

   ```yaml
   spring:
     servlet:
       multipart:
         max-file-size: 100MB
         max-request-size: 100MB
   ```

2. 检查上传目录权限：

   ```bash
   chmod 755 /opt/ich-system/files
   ```

3. 检查磁盘空间：
   ```bash
   df -h
   ```

### 12.6 性能问题

#### 问题 1：接口响应慢

**解决方案**：

1. 添加 Redis 缓存
2. 优化数据库查询
3. 添加索引
4. 使用异步处理

#### 问题 2：内存溢出

**解决方案**：

1. 调整 JVM 参数：

   ```bash
   java -Xms512m -Xmx2g -jar demo.jar
   ```

2. 检查内存泄漏
3. 优化大对象处理

---

## 附录

### A. 项目目录结构

```
BS/
├── demo/                          # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/example/demo/
│   │   │   │       ├── aop/              # AOP 切面
│   │   │   │       ├── common/           # 公共类
│   │   │   │       ├── config/           # 配置类
│   │   │   │       ├── controller/       # 控制器
│   │   │   │       ├── entity/           # 实体类
│   │   │   │       ├── interceptor/      # 拦截器
│   │   │   │       ├── mapper/           # Mapper
│   │   │   │       ├── request/          # 请求对象
│   │   │   │       ├── service/          # 服务层
│   │   │   │       ├── util/             # 工具类
│   │   │   │       └── websocket/        # WebSocket
│   │   │   └── resources/
│   │   │       ├── application.yml       # 配置文件
│   │   │       └── mapper/               # MyBatis XML
│   │   └── test/                         # 测试代码
│   ├── files/                            # 上传文件
│   ├── pom.xml                           # Maven 配置
│   └── HELP.md                           # 帮助文档
│
└── ich-frontend/                         # 前端项目
    ├── public/                           # 公共文件
    ├── src/
    │   ├── components/                   # 组件
    │   ├── router/                       # 路由
    │   ├── utils/                        # 工具
    │   ├── views/                        # 页面
    │   ├── App.vue                       # 根组件
    │   └── main.ts                       # 入口
    ├── index.html                        # HTML 模板
    ├── package.json                      # 依赖配置
    ├── vite.config.js                    # Vite 配置
    └── README.md                         # 说明文档
```

### B. 常用命令速查

#### 后端命令

```bash
# 编译项目
mvn clean install

# 运行项目
mvn spring-boot:run

# 打包项目
mvn clean package -DskipTests

# 运行 JAR
java -jar target/demo-0.0.1-SNAPSHOT.jar

# 查看日志
tail -f logs/app.log
```

#### 前端命令

```bash
# 安装依赖
npm install

# 开发模式
npm run dev

# 构建生产版本
npm run build

# 预览构建
npm run preview

# 清理缓存
npm run clean
```

#### 数据库命令

```bash
# 登录 MySQL
mysql -u root -p

# 导出数据库
mysqldump -u root -p ich_promotion_db > backup.sql

# 导入数据库
mysql -u root -p ich_promotion_db < backup.sql
```

### C. 联系方式

- 项目仓库：[GitHub 仓库地址]
- 问题反馈：[Issue 地址]
- 开发团队：[团队名称]

---

**文档版本**：v1.0  
**最后更新**：2026-03-02  
**维护人员**：开发团队
