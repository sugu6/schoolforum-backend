package com.example.schoolforum.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "通知类型枚举", example = "COMMENT")
public enum NotificationType {

    COMMENT("COMMENT", "评论通知"),
    REPLY("REPLY", "回复通知"),
    LIKE("LIKE", "点赞通知"),
    FOLLOW("FOLLOW", "关注通知"),
    UNFOLLOW("UNFOLLOW", "取关通知"),
    SYSTEM("SYSTEM", "系统通知"),
    ESSENTIAL("ESSENTIAL", "精华通知"),
    PINNED("PINNED", "置顶通知");

    @EnumValue
    private final String code;
    @Schema(hidden = true)
    private final String desc;

    NotificationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}
