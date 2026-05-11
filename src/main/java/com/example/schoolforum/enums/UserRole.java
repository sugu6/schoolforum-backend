package com.example.schoolforum.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserRole {

    SUPER_ADMIN(0, "超级管理员"),
    ADMIN(1, "管理员"),
    USER(2, "普通用户");

    @EnumValue
    private final Integer code;
    private final String desc;

    UserRole(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}
