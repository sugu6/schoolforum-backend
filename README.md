# 海语后端 (Haiyu Backend)

<p align="center">
  <strong>校园论坛社区平台 - 后端服务</strong>
</p>

---

## 项目简介

海语后端是校园论坛社区平台的服务端，基于 Spring Boot 构建，提供用户认证、帖子管理、评论系统、实时消息等功能的 RESTful API。

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| [Spring Boot](https://spring.io/projects/spring-boot) | 3.5.11 | 应用框架 |
| [Java](https://openjdk.org/) | 17 | 运行环境 |
| [MyBatis-Flex](https://mybatis-flex.com/) | 1.11.6 | ORM 框架 |
| [MySQL](https://www.mysql.com/) | 9.6 | 关系型数据库 |
| [Redis](https://redis.io/) | - | 缓存/会话存储 |
| [Sa-Token](https://sa-token.cc/) | 1.44.0 | 认证授权 |
| [Manticore Search](https://manticoresearch.com/) | - | 全文搜索引擎 |
| [JustAuth](https://github.com/JustAuth/JustAuth) | 1.16.7 | OAuth 登录 |

---

## 功能特性

- **用户认证**：基于 Sa-Token + JWT 的双 Token 认证机制
- **帖子管理**：发帖、编辑、删除、置顶、精华、分类标签
- **评论系统**：多级评论回复
- **实时推送**：WebSocket 消息推送
- **全文搜索**：Manticore Search 支持
- **文件上传**：头像、帖子图片管理
- **OAuth 登录**：GitHub 第三方登录

---

## 快速开始

### 环境要求

- Java 17+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 安装步骤

1. **克隆项目**

```bash
git clone <repository-url>
cd schoolforum-backend
```

2. **配置数据库**

创建数据库并导入表结构：

```bash
mysql -u root -p < sql/schoolforum.sql
```

3. **修改配置**

复制并修改配置文件：

```bash
cp src/main/resources/application-dev.yml src/main/resources/application-local.yml
```

根据实际情况修改数据库连接、Redis 等配置。

4. **启动应用**

```bash
# 使用 Maven
mvn spring-boot:run

# 或打包后运行
mvn clean package -DskipTests
java -jar target/schoolforum-0.0.1-SNAPSHOT.jar
```

5. **访问 API 文档**

启动后访问 Swagger UI：`http://localhost:8085/swagger-ui.html`

---

## 项目结构

```
schoolforum-backend/
├── src/main/java/com/example/schoolforum/
│   ├── controller/        # REST API 端点
│   ├── service/           # 业务逻辑层
│   │   └── impl/          # 服务实现
│   ├── mapper/            # MyBatis-Flex Mapper
│   ├── pojo/              # 实体类
│   │   ├── dto/           # 数据传输对象
│   │   └── common/        # 通用类 (Result)
│   ├── config/            # 配置类
│   ├── enums/             # 枚举类
│   ├── exception/         # 自定义异常
│   ├── util/              # 工具类
│   ├── component/         # 组件
│   ├── event/             # 事件
│   ├── websocket/         # WebSocket 处理
│   └── SchoolforumApplication.java
├── src/main/resources/
│   ├── application-dev.yml
│   └── application-prod.yml
├── sql/                   # 数据库脚本
├── nginx/                 # Nginx 配置
├── ssl/                   # SSL 证书
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

---

## 常用命令

```bash
# 构建项目
mvn clean package

# 运行测试
mvn test

# 运行单个测试
mvn test -Dtest=SchoolforumApplicationTests

# 构建时跳过测试
mvn clean package -DskipTests

# 运行应用
mvn spring-boot:run
```

---

## Docker 部署

### 使用 Docker Compose

```bash
# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f schoolforum

# 停止服务
docker-compose down
```

### 环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `DB_URL` | 数据库连接 URL | `jdbc:mysql://host:3306/schoolforum` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | - |
| `REDIS_HOST` | Redis 地址 | `127.0.0.1` |
| `REDIS_PASSWORD` | Redis 密码 | - |
| `JWT_SECRET_KEY` | JWT 密钥（至少32字符） | - |
| `GITHUB_CLIENT_ID` | GitHub OAuth Client ID | - |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth Client Secret | - |

---

## API 端点

### 认证相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/users/login` | 用户登录 |
| POST | `/users/register` | 用户注册 |
| POST | `/users/logout` | 用户登出 |
| POST | `/auth/refresh` | 刷新 Token |

### 帖子相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/posts/add` | 发布帖子 |
| GET | `/posts/list` | 获取帖子列表 |
| GET | `/posts/get/{id}` | 获取帖子详情 |
| PUT | `/posts/update/{id}` | 更新帖子 |
| DELETE | `/posts/delete/{id}` | 删除帖子 |

### 评论相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/comments/add` | 添加评论 |
| GET | `/comments/list/post/{postId}` | 获取帖子评论 |

### 用户相关

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/users/getInfo` | 获取当前用户信息 |
| PUT | `/users/update` | 更新用户信息 |
| POST | `/users/uploadAvatar` | 上传头像 |

---

## 开发注意事项

- 应用运行端口：**8085**
- 数据库凭证和 OAuth 密钥在配置文件中，**请勿提交敏感信息**
- 生产环境必须配置 `JWT_SECRET_KEY` 环境变量
- Swagger UI 在生产环境中已禁用

---

## 许可证

本项目采用 [MIT](LICENSE) 许可证。
