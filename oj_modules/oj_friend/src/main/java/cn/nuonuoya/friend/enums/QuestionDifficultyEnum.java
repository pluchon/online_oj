package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 题目难度枚举
@Getter
public enum QuestionDifficultyEnum {

    // 简单
    EASY(1, "简单"),

    // 中等
    MEDIUM(2, "中等"),

    // 困难
    HARD(3, "困难");

    // 未匹配时的描述
    private static final String UNKNOWN_DESC = "未知";

    // 难度编码
    private final Integer code;

    // 难度描述
    private final String desc;

    QuestionDifficultyEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据难度编码获取描述，未匹配返回"未知"
    public static String getDescByCode(Integer code) {
        if (code == null) {
            return UNKNOWN_DESC;
        }
        for (QuestionDifficultyEnum difficulty : values()) {
            if (difficulty.getCode().equals(code)) {
                return difficulty.getDesc();
            }
        }
        return UNKNOWN_DESC;
    }
}
