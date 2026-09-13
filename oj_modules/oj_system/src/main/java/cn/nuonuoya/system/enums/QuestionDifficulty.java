package cn.nuonuoya.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 题目难度枚举
@AllArgsConstructor
@Getter
public enum QuestionDifficulty {

    // 简单
    EASY(1, "简单"),
    // 中等
    MEDIUM(2, "中等"),
    // 困难
    HARD(3, "困难");

    // 难度数值
    private final int value;

    // 难度描述
    private final String desc;

    // 根据数值获取描述
    public static String getDescByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (QuestionDifficulty item : values()) {
            if (item.getValue() == value) {
                return item.getDesc();
            }
        }
        return null;
    }
}
