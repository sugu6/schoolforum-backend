package com.example.schoolforum.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum DeletionStatus {

    PENDING(0, "申请注销"),
    CANCELLED(1, "撤销注销"),
    COMPLETED(2, "注销完成");

    @EnumValue
    private final Integer code;
    private final String desc;

    DeletionStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}