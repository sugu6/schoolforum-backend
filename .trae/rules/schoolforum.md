# SchoolForum 项目开发规则

## 项目概述

这是一个基于 Spring Boot 3.5.11 的校园论坛系统，使用 Java 17 开发。

## 技术栈

- **框架**: Spring Boot 3.5.11 + Java 17
- **ORM**: MyBatis-Flex 1.11.6
- **数据库**: MySQL 9.6 + HikariCP 5.1.0 连接池
- **认证授权**: Sa-Token 1.44.0 + JWT
- **缓存**: Redis
- **API文档**: SpringDoc OpenAPI 2.8.15 (Swagger UI: `/swagger-ui.html`)
- **密码加密**: BCrypt (Spring Security Crypto)
- **邮件**: Spring Mail
- **OAuth**: JustAuth 1.16.7 (支持 GitHub 登录)
- **工具**: Lombok

## 构建与运行命令

```bash
# 构建项目
mvn clean package

# 运行应用（端口 8085）
mvn spring-boot:run

# 运行测试
mvn test

# 构建时跳过测试
mvn clean package -DskipTests
```

## 项目结构

```
src/main/java/com/example/schoolforum/
├── controller/     # REST API 控制器
├── service/        # 服务接口
├── service/impl/   # 服务实现
├── mapper/         # MyBatis-Flex Mapper 接口
├── pojo/           # 实体类
│   ├── dto/        # 数据传输对象
│   └── common/     # 通用类（如 Result）
├── config/         # 配置类
├── enums/          # 枚举类
├── exception/      # 自定义异常
├── util/           # 工具类
└── SchoolforumApplication.java
```

## 代码规范

### 1. 注释规则
- **禁止删除注释**，除非明确要求删除
- 类和公共方法必须有 Javadoc 注释
- 复杂逻辑需要添加行内注释说明

### 2. 命名规范
- **包名**: 全小写，如 `com.example.schoolforum`
- **类名**: 大驼峰命名，如 `UsersController`
- **方法名**: 小驼峰命名，如 `getUserInfo`
- **常量**: 全大写下划线分隔，如 `CAPTCHA_EXPIRE_TIME`
- **枚举**: 大驼峰命名，如 `UserRole`

### 3. Lombok 使用规范
- 实体类使用 `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- 依赖注入使用 `@RequiredArgsConstructor` 配合 `final` 字段
- 日志使用 `@Slf4j`
- Service 实现类继承 `ServiceImpl<Mapper, Entity>`

### 4. API 文档规范
所有 Controller 必须添加 Swagger 注解：
- 类级别: `@Tag(name = "模块名称", description = "模块描述")`
- 方法级别: `@Operation(summary = "简短描述", description = "详细描述")`
- 参数: `@Parameter(description = "参数描述")`
- 实体字段: `@Schema(description = "字段描述", example = "示例值")`

### 5. 统一响应格式
项目通过 `ResponseAdvice` 实现响应自动封装，Controller 可直接返回原始数据类型：

```java
// 直接返回原始类型，ResponseAdvice 会自动封装为 Result.success(data)
@GetMapping("/user/{id}")
public User getUser(@PathVariable Long id) {
    return userService.getById(id);
}

// 返回 void 时，自动封装为 Result.success(null)
@DeleteMapping("/user/{id}")
public void deleteUser(@PathVariable Long id) {
    userService.removeById(id);
}

// 返回 String 时，自动封装为 Result.success("字符串内容")
@GetMapping("/hello")
public String hello() {
    return "Hello World";
}
```

**自动封装规则**（由 `ResponseAdvice` 处理）：
- 返回非 `Result` 类型 → 自动封装为 `Result.success(data)`
- 返回 `String` 类型 → 自动封装并设置 Content-Type 为 JSON
- 返回 `void` → 自动封装为 `Result.success(null)`
- 返回 `Result` 类型 → 直接返回，不再封装

**失败响应**：抛出 `BusinessException`，由全局异常处理器统一处理
```java
throw new BusinessException("错误信息");
```

### 6. 异常处理
- 业务异常抛出 `BusinessException`
- 全局异常处理器 `GlobalExceptionHandler` 统一处理
- 不要在 Controller 中捕获异常，交给全局处理器

### 7. 认证与授权
- 使用 Sa-Token 的注解进行权限控制：
  - `@SaCheckLogin`: 需要登录
  - `@SaCheckRole({"admin", "super_admin"})`: 需要指定角色
- 公开接口在 `SaTokenConfig.getExcludePatterns()` 中配置

### 8. 数据库操作
- 使用 MyBatis-Flex 的 `QueryWrapper` 构建查询
- 分页使用 `Page<T>` 和 `paginate` 方法
- 主键使用 `@Id(keyType = KeyType.Auto)` 自增
- 枚举字段使用 `@EnumValue` 标记存储值
- **直接数据库操作**: 需要直接查询或操作数据库时，优先使用 MySQL MCP 工具（详见"MCP 工具使用规范"章节）

### 9. 安全规范
- 密码必须使用 `PasswordEncoder` 加密，禁止明文存储
- 返回用户信息前清除密码字段: `user.setPassword(null)`
- 敏感配置（数据库密码、OAuth密钥等）不要提交到代码库
- 验证码等临时数据存储在 Redis 中并设置过期时间

### 10. 枚举类规范
```java
@Getter
public enum UserRole {
    SUPER_ADMIN(0, "超级管理员"),
    ADMIN(1, "管理员"),
    USER(2, "普通用户");

    @EnumValue
    private final Integer code;
    private final String desc;

    UserRole(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}
```

## 开发流程

### 新增功能步骤
1. 在 `pojo` 包创建实体类
2. 在 `mapper` 包创建 Mapper 接口
3. 在 `service` 包创建 Service 接口
4. 在 `service/impl` 包创建 Service 实现
5. 在 `controller` 包创建 Controller
6. 添加 Swagger 注解
7. 在 `SaTokenConfig` 中配置公开接口（如需要）

### 数据库变更
- 直接执行 SQL 脚本或使用数据库管理工具进行变更
- 建议在开发环境先测试 SQL 语句，确认无误后再执行

## 配置文件

- 应用配置: `src/main/resources/application.yml`
- 应用端口: **8085**
- 数据库: MySQL (127.0.0.1:3306/schoolforum)
- Redis: 127.0.0.1:6379

## 测试规范

- 测试类放在 `src/test/java` 下，保持相同的包结构
- 测试类命名: `{类名}Test`
- 使用 `@SpringBootTest` 进行集成测试

## 注意事项

1. 不要删除现有注释
2. 遵循现有的代码风格和架构模式
3. 新增 API 必须添加 Swagger 文档注解
4. 敏感信息不要硬编码或提交到版本控制
5. 使用构造函数注入而非字段注入
6. Service 层方法要有清晰的职责划分
7. 复杂业务逻辑需要添加注释说明

## MCP 工具使用规范

### 1. 主动调用原则
在开发过程中，应主动使用 MCP 工具提高开发效率和代码质量：

- **数据库操作**: 使用 `mcp_mysql_*` 工具直接查询和操作数据库
- **浏览器测试**: 使用 `mcp_Chrome_DevTools_MCP_*` 工具进行前端测试和调试
- **文档查询**: 使用 `mcp_context7_*` 工具查询第三方库的最新文档

### 2. MySQL MCP 工具
用于直接操作项目数据库：

```
# 连接数据库
mcp_mysql_connect_db - 连接到 MySQL 数据库

# 查询操作
mcp_mysql_query - 执行 SELECT 查询
mcp_mysql_execute - 执行 INSERT/UPDATE/DELETE 操作
mcp_mysql_list_tables - 列出所有表
mcp_mysql_describe_table - 查看表结构
```

**使用场景**：
- 验证数据库表结构和字段
- 快速查询数据验证业务逻辑
- 调试时检查数据状态
- 执行数据修复操作

### 3. Chrome DevTools MCP 工具
用于前端页面测试和调试：

```
# 页面导航
mcp_Chrome_DevTools_MCP_navigate_page - 导航到指定 URL
mcp_Chrome_DevTools_MCP_new_page - 打开新标签页
mcp_Chrome_DevTools_MCP_list_pages - 列出所有打开的页面

# 页面交互
mcp_Chrome_DevTools_MCP_click - 点击元素
mcp_Chrome_DevTools_MCP_fill - 填写表单
mcp_Chrome_DevTools_MCP_take_snapshot - 获取页面快照
mcp_Chrome_DevTools_MCP_take_screenshot - 截取页面截图

# 网络和调试
mcp_Chrome_DevTools_MCP_list_network_requests - 查看网络请求
mcp_Chrome_DevTools_MCP_list_console_messages - 查看控制台消息
mcp_Chrome_DevTools_MCP_evaluate_script - 执行 JavaScript

# 性能分析
mcp_Chrome_DevTools_MCP_performance_start_trace - 开始性能追踪
mcp_Chrome_DevTools_MCP_lighthouse_audit - 运行 Lighthouse 审计
```

**使用场景**：
- 测试 API 接口的前端交互
- 验证页面渲染效果
- 调试前端 JavaScript 问题
- 分析页面性能问题

### 4. Context7 MCP 工具
用于查询第三方库的最新文档：

```
mcp_context7_resolve-library-id - 解析库 ID
mcp_context7_query-docs - 查询库文档
```

**使用场景**：
- 查询 Spring Boot、MyBatis-Flex 等框架的最新用法
- 获取第三方库的 API 文档和示例代码
- 解决版本兼容性问题

### 5. 最佳实践

1. **数据库调试优先**: 遇到数据相关问题时，优先使用 MySQL MCP 工具直接查询验证
2. **前端测试自动化**: 使用 Chrome DevTools MCP 工具自动化前端测试流程
3. **文档即时查询**: 不确定 API 用法时，主动使用 Context7 查询最新文档
4. **性能问题排查**: 使用性能追踪工具定位前端性能瓶颈
5. **网络请求分析**: 通过网络请求列表分析 API 调用情况
