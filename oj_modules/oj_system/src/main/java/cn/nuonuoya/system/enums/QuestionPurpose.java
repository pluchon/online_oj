package cn.nuonuoya.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 题目用途枚举（刷题题出现在 C 端题库，竞赛题只能从竞赛进入）
@AllArgsConstructor
@Getter
public enum QuestionPurpose {

    // 刷题
    PRACTICE(1, "刷题"),
    // 竞赛
    CONTEST(2, "竞赛");

    // 用途数值
    private final int value;

    // 用途描述
    private final String desc;

    // 根据数值获取描述
    public static String getDescByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (QuestionPurpose item : values()) {
            if (item.getValue() == value) {
                return item.getDesc();
            }
        }
        return null;
    }
}
