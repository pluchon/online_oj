package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 时间范围筛选枚举
@Getter
public enum TimeRangeEnum {

    // 全部时间
    ALL("all", "全部时间"),

    // 近一年
    YEAR("year", "近一年"),

    // 近一月
    MONTH("month", "近一月"),

    // 本周
    WEEK("week", "本周");

    // 筛选编码
    private final String code;

    // 描述信息
    private final String desc;

    TimeRangeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据编码解析枚举，未匹配默认返回 ALL
    public static TimeRangeEnum of(String code) {
        if (code == null || code.trim().isEmpty()) {
            return ALL;
        }
        for (TimeRangeEnum range : values()) {
            if (range.getCode().equalsIgnoreCase(code.trim())) {
                return range;
            }
        }
        return ALL;
    }
}
