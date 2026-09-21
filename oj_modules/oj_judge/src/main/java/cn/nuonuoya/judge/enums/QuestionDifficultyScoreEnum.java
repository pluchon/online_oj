package cn.nuonuoya.judge.enums;

import lombok.Getter;

// 题目难度满分枚举（按难度确定全部用例通过时的得分）
@Getter
public enum QuestionDifficultyScoreEnum {

    // 简单
    EASY(1, 100),

    // 中等
    MEDIUM(2, 200),

    // 困难
    HARD(3, 300);

    // 难度编码
    private final Integer code;

    // 满分
    private final int fullScore;

    QuestionDifficultyScoreEnum(Integer code, int fullScore) {
        this.code = code;
        this.fullScore = fullScore;
    }

    // 根据难度编码获取满分，未匹配按简单题计分
    public static int getFullScore(Integer code) {
        for (QuestionDifficultyScoreEnum difficulty : values()) {
            if (difficulty.getCode().equals(code)) {
                return difficulty.getFullScore();
            }
        }
        return EASY.getFullScore();
    }
}
