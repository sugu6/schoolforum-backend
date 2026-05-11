package com.example.schoolforum.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ActiveStatus {

    INACTIVE(0, "未激活"),
    ACTIVE(1, "已激活");

    @EnumValue
    private final Integer code;
    private final String desc;

    ActiveStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}
