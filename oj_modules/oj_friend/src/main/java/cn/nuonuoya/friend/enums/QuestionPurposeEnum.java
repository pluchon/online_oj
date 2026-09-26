package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 题目用途枚举（刷题题出现在题库，竞赛题只能从竞赛进入，且不提供题解与 AI 辅导）
@Getter
public enum QuestionPurposeEnum {

    // 刷题
    PRACTICE(1, "刷题"),

    // 竞赛
    CONTEST(2, "竞赛");

    // 用途编码
    private final Integer code;

    // 用途描述
    private final String desc;

    QuestionPurposeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 是否为竞赛题
    public static boolean isContest(Integer code) {
        return CONTEST.getCode().equals(code);
    }
}
