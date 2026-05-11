package com.example.schoolforum.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "关联类型枚举", example = "POST")
public enum RelatedType {

    POST("POST", "帖子"),
    COMMENT("COMMENT", "评论");

    @EnumValue
    private final String code;
    @Schema(hidden = true)
    private final String desc;

    RelatedType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}
