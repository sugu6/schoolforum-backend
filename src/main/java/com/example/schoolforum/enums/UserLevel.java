package com.example.schoolforum.enums;

import lombok.Getter;

@Getter
public enum UserLevel {

    LV1(1, 0, "小白"),
    LV2(2, 200, "萌新"),
    LV3(3, 1000, "老司机"),
    LV4(4, 3500, "资深"),
    LV5(5, 10000, "达人"),
    LV6(6, 30000, "大佬");

    private final Integer level;
    private final Integer requiredExp;
    private final String title;

    UserLevel(Integer level, Integer requiredExp, String title) {
        this.level = level;
        this.requiredExp = requiredExp;
        this.title = title;
    }

    public static UserLevel getByExp(Integer exp) {
        if (exp == null || exp < 0) {
            return LV1;
        }
        UserLevel result = LV1;
        for (UserLevel userLevel : values()) {
            if (exp >= userLevel.getRequiredExp()) {
                result = userLevel;
            } else {
                break;
            }
        }
        return result;
    }

    public static Integer calculateLevel(Integer exp) {
        return getByExp(exp).getLevel();
    }

    public static Integer getExpToNextLevel(Integer currentExp) {
        UserLevel current = getByExp(currentExp);
        if (current == LV6) {
            return 0;
        }
        UserLevel next = values()[current.ordinal() + 1];
        return next.getRequiredExp() - currentExp;
    }
}
