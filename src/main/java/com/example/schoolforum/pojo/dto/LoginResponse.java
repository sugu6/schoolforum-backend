package com.example.schoolforum.pojo.dto;

import com.example.schoolforum.pojo.Users;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录响应 DTO
 * 
 * refreshToken 不在此对象中返回，而是通过 httpOnly Cookie 由服务端直接设置到浏览器。
 * 这确保了 refreshToken 永远不会通过 JSON 响应体暴露给 JavaScript。
 *
 * @author sugu
 */
@Schema(description = "登录响应")
public record LoginResponse(
    @Schema(description = "用户信息")
    Users user, 

    @Schema(description = "access token 有效期（秒）")
    long expiresIn
) {}
