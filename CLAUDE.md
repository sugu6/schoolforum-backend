# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码仓库中工作提供指导。

## 构建与开发命令

```bash
# 构建项目
mvn clean package

# 运行应用
mvn spring-boot:run

# 运行测试
mvn test

# 运行单个测试
mvn test -Dtest=SchoolforumApplicationTests

# 构建时跳过测试
mvn clean package -DskipTests
```

## 架构概述

**技术栈:**
- Spring Boot 3.5.11 + Java 17
- MyBatis-Flex 1.11.6 (ORM 框架)
- MySQL 9.6 + HikariCP 5.1.0 连接池
- Sa-Token 1.44.0 + JWT (认证/授权)
- Redis (会话/缓存)
- JustAuth 1.16.7 (OAuth - GitHub 登录)
- SpringDoc OpenAPI 2.8.15 (Swagger UI 访问 `/swagger-ui.html`)
- BCrypt (密码加密)
- Spring Mail (邮件验证)

**项目结构:**
```
src/main/java/com/example/schoolforum/
├── controller/     # REST API 端点 (Users, Posts, Comments, OAuth)
├── service/        # 业务逻辑层
├── service/impl/   # 服务实现
├── mapper/         # MyBatis-Flex Mapper
├── pojo/           # 实体类 (Users, Posts, Comments)
│   ├── dto/        # 数据传输对象 (PostCreateRequest 等)
│   └── common/     # 通用类 (Result)
├── config/         # 配置类 (SaToken, OpenApi, Password, GitHub OAuth)
├── enums/          # 枚举类 (UserRole, ActiveStatus, CodeType)
├── exception/      # 自定义异常 (BusinessException)
├── util/           # 工具类
└── SchoolforumApplication.java
```

**核心模式:**
- 标准 Spring Boot 分层架构：Controller → Service → Mapper
- 统一响应格式：所有 API 响应使用 `Result<T>` 包装
- 全局异常处理：通过 `GlobalExceptionHandler`
- Sa-Token JWT 认证，基于拦截器的鉴权 (见 `SaTokenConfig`)
- 密码加密：通过 `PasswordEncoder` Bean
- Lombok 简化代码：`@RequiredArgsConstructor`、`@Data` 等注解

**认证流程:**
- 基于 Sa-Token 的 JWT 认证 (`StpLogicJwtForSimple`)
- 公开端点在 `SaTokenConfig.addInterceptors()` 中排除
- Session 存储用户角色用于权限检查
- 支持 GitHub OAuth 登录 (通过 JustAuth)

**数据库:**
- MyBatis-Flex ORM，基于注解的映射
- 分页通过 `Page<T>` 和 `QueryWrapper` 实现
- 自增主键：`@Id(keyType = KeyType.Auto)`

## 开发注意事项

- 应用运行端口：**8085** (在 `application.yml` 中配置)
- 数据库凭证和 OAuth 密钥在 `application.yml` 中 - 请勿提交敏感信息
- Swagger UI 已启用，根路径重定向到 `/swagger-ui.html`
- 代码生成工具可通过 `mybatis-flex-codegen` 依赖使用

# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
