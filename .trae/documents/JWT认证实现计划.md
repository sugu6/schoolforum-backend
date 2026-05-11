# JWT Token认证实现计划

## 一、实现目标

为校园论坛系统实现完整的JWT Token认证机制，支持无状态的用户认证。

## 二、技术方案

- **JWT库**: 使用 `jjwt` (io.jsonwebtoken:jjwt-api)
- **Token类型**: Access Token
- **签名算法**: HS256
- **Token存储**: 前端存储在localStorage/sessionStorage，请求时放在Authorization头中

## 三、实现步骤

### 步骤1：添加JWT依赖
在 `pom.xml` 中添加jjwt相关依赖

### 步骤2：创建JWT工具类
创建 `JwtUtils.java`，提供以下功能：
- 生成Token
- 解析Token
- 验证Token有效性
- 从Token中获取用户信息

### 步骤3：创建JWT认证过滤器
创建 `JwtAuthenticationFilter.java`：
- 从请求头获取Token
- 验证Token有效性
- 设置认证信息到SecurityContext

### 步骤4：修改Security配置
修改 `SecurityConfig.java`：
- 配置JWT过滤器
- 添加JWT相关配置

### 步骤5：修改登录接口
修改 `AuthController.java`：
- 登录成功后返回JWT Token
- 创建登录响应DTO

### 步骤6：添加JWT配置
在 `application.yml` 中添加JWT配置项

## 四、涉及文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `pom.xml` | 修改 | 添加JWT依赖 |
| `application.yml` | 修改 | 添加JWT配置 |
| `security/JwtUtils.java` | 新增 | JWT工具类 |
| `security/JwtAuthenticationFilter.java` | 新增 | JWT过滤器 |
| `config/SecurityConfig.java` | 修改 | 配置JWT过滤器 |
| `controller/AuthController.java` | 修改 | 返回Token |
| `dto/LoginResponseDTO.java` | 新增 | 登录响应DTO |

## 五、API变更

### 登录接口响应变更

**原响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "username": "test",
    "email": "test@example.com",
    ...
  }
}
```

**新响应**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "test",
      "email": "test@example.com",
      ...
    }
  }
}
```

## 六、认证流程

```
1. 用户登录
   └─> 验证用户名密码
   └─> 生成JWT Token
   └─> 返回Token和用户信息

2. 后续请求
   └─> 请求头携带: Authorization: Bearer {token}
   └─> JwtAuthenticationFilter拦截
   └─> 解析验证Token
   └─> 设置认证信息
   └─> 继续请求处理
```
