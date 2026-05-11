package com.example.schoolforum.enums;

import lombok.Getter;

@Getter
public enum CodeType {
    REGISTER("register", "注册账号"),
    CHANGE_PASSWORD("changePassword", "修改密码"),
    RESET_PASSWORD("resetPassword", "重置密码");

    private final String code;
    private final String desc;

    CodeType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
