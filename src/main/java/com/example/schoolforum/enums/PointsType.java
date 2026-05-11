package com.example.schoolforum.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PointsType {

    SIGN("SIGN", "签到"),
    POST("POST", "发帖"),
    COMMENT("COMMENT", "评论"),
    LIKE_RECEIVED("LIKE_RECEIVED", "获赞"),
    EXCHANGE_SIGN_CARD("EXCHANGE_SIGN_CARD", "兑换补签卡"),
    ADMIN_REWARD("ADMIN_REWARD", "管理员奖励"),
    ADMIN_DEDUCT("ADMIN_DEDUCT", "管理员扣除");

    @EnumValue
    private final String code;
    private final String desc;

    PointsType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}
