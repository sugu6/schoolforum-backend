package com.example.schoolforum.pojo.dto;

import com.example.schoolforum.pojo.Users;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户搜索文档")
public class UserSearchDocument {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "用户角色")
    private String role;

    @Schema(description = "激活状态")
    private Integer isActive;

    @Schema(description = "创建时间")
    private String createdAt;

    public static UserSearchDocument fromEntity(Users user) {
        return UserSearchDocument.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .age(user.getAge())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .role(user.getRole() != null ? user.getRole().name() : null)
                .isActive(user.getIsActive() != null ? user.getIsActive().getCode() : 0)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }
}
