package com.example.schoolforum.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 热门榜时间窗口枚举
 * 支持多种时间维度的热门榜单
 */
@Getter
@AllArgsConstructor
public enum TimeWindow {

    HOUR_24(24, "24小时热门"),
    DAY_7(168, "7天热门"),
    DAY_30(720, "30天热门"),
    ALL(Integer.MAX_VALUE, "历史最热");

    private final int hours;
    private final String description;

    /**
     * 根据字符串名称获取枚举值
     * @param name 窗口名称（不区分大小写）
     * @return 对应的 TimeWindow 枚举，默认返回 ALL
     */
    public static TimeWindow fromString(String name) {
        if (name == null || name.isBlank()) {
            return ALL;
        }
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }
}
