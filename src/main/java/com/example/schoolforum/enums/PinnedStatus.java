package com.example.schoolforum.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PinnedStatus {

    NOT_PINNED(0, "未置顶"),
    PINNED(1, "已置顶");

    @EnumValue
    private final Integer code;
    private final String desc;

    PinnedStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}
