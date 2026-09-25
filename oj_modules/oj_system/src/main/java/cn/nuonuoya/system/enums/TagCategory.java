package cn.nuonuoya.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 标签分类枚举（C 端能力雷达图按分类统计）
@AllArgsConstructor
@Getter
public enum TagCategory {

    // 数据结构
    DATA_STRUCTURE(1, "数据结构"),
    // 算法
    ALGORITHM(2, "算法"),
    // 数学
    MATH(3, "数学"),
    // 其他
    OTHER(4, "其他");

    // 分类数值
    private final int value;

    // 分类描述
    private final String desc;

    // 根据数值获取描述
    public static String getDescByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (TagCategory item : values()) {
            if (item.getValue() == value) {
                return item.getDesc();
            }
        }
        return null;
    }
}
