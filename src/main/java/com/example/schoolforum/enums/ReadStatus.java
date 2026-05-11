package com.example.schoolforum.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "已读状态枚举", example = "UNREAD")
public enum ReadStatus {

    UNREAD(0, "未读"),
    READ(1, "已读");

    @EnumValue
    private final Integer code;
    @Schema(hidden = true)
    private final String desc;

    ReadStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}
