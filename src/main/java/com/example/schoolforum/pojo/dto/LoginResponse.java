package com.example.schoolforum.pojo.dto;

import com.example.schoolforum.pojo.Users;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录响应")
public record LoginResponse(
    @Schema(description = "用户信息")
    Users user, 
    
    @Schema(description = "登录令牌")
    String token
) {}
