package com.example.schoolforum.pojo.dto;

import com.example.schoolforum.pojo.Users;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * OAuth 回调响应 DTO
 * 
 * refreshToken 不在此对象中返回，而是通过 httpOnly Cookie 由服务端直接设置到浏览器。
 * 这确保了 refreshToken 永远不会通过 JSON 响应体暴露给 JavaScript。
 *
 * @author sugu
 */
@Schema(description = "OAuth回调响应")
public record OAuthCallbackResult(
    
    @Schema(description = "状态: success-登录成功, conflict-用户名冲突需要选择")
    String status,
    
    @Schema(description = "登录成功时的用户信息")
    Users user,
    
    @Schema(description = "access token 有效期（秒）")
    long expiresIn,
    
    @Schema(description = "GitHub用户临时标识(用于确认用户名)")
    String tempKey,
    
    @Schema(description = "冲突时的原用户名")
    String suggestedUsername,
    
    @Schema(description = "建议的用户名列表")
    List<String> suggestedUsernames,
    
    @Schema(description = "GitHub用户邮箱")
    String email,
    
    @Schema(description = "GitHub用户头像")
    String avatarUrl
) {
    
    public static OAuthCallbackResult success(Users user, long expiresIn) {
        return new OAuthCallbackResult("success", user, expiresIn, null, null, null, null, null);
    }
    
    public static OAuthCallbackResult conflict(String tempKey, String suggestedUsername, 
                                                List<String> suggestedUsernames, 
                                                String email, String avatarUrl) {
        return new OAuthCallbackResult("conflict", null, 0, tempKey, suggestedUsername, 
                                        suggestedUsernames, email, avatarUrl);
    }
}
