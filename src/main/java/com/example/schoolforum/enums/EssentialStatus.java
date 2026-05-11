package com.example.schoolforum.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum EssentialStatus {

    NOT_ESSENTIAL(0, "非精华"),
    ESSENTIAL(1, "精华");

    @EnumValue
    private final Integer code;
    private final String desc;

    EssentialStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}
