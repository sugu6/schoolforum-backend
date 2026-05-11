package com.example.schoolforum.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "公告类型枚举")
public enum AnnouncementType {

    INFO("INFO", "普通通知"),
    IMPORTANT("IMPORTANT", "重要公告"),
    URGENT("URGENT", "紧急公告");

    @EnumValue
    private final String code;
    @Schema(hidden = true)
    private final String desc;

    AnnouncementType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}
