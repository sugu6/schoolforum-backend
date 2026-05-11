package com.example.schoolforum.pojo;

import com.example.schoolforum.enums.ActiveStatus;
import com.example.schoolforum.enums.Gender;
import com.example.schoolforum.enums.UserRole;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
@Schema(description = "用户实体")
public class Users implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "年龄", example = "25")
    private Integer age;

    @Schema(description = "性别", example = "MALE")
    @Builder.Default
    private Gender gender = Gender.SECRET;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "个人简介", example = "热爱编程，喜欢分享技术")
    private String bio;

    @Schema(description = "GitHub用户ID", example = "1234567")
    private String githubId;

    @Schema(description = "用户角色", example = "USER")
    private UserRole role;

    @Schema(description = "激活状态", example = "ACTIVE")
    private ActiveStatus isActive;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;

    @Schema(description = "是否公开关注列表", example = "false")
    @Builder.Default
    private Boolean showFollowing = false;

    @Schema(description = "是否公开粉丝列表", example = "false")
    @Builder.Default
    private Boolean showFollowers = false;

    @Schema(description = "用户等级(1-6)", example = "1")
    @Builder.Default
    private Integer level = 1;

    @Schema(description = "经验值", example = "0")
    @Builder.Default
    private Integer exp = 0;

    @Schema(description = "积分余额", example = "0")
    @Builder.Default
    private Integer points = 0;

    @Schema(description = "连续签到天数", example = "0")
    @Builder.Default
    private Integer continuousSignDays = 0;

    @Schema(description = "最后签到日期", example = "2024-01-01")
    private LocalDate lastSignDate;

    @Schema(description = "补签卡数量", example = "0")
    @Builder.Default
    private Integer signCards = 0;

}
