package cn.nuonuoya.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// AI 帮建竞赛的难度倾向（简单 : 中等 : 困难 的配比）
@Getter
@AllArgsConstructor
public enum ExamAiTendency {

    // 新手友好
    NOVICE(1, "新手友好", new int[]{6, 4, 0}),

    // 一般大众
    GENERAL(2, "一般大众", new int[]{3, 5, 2}),

    // 高手过招
    EXPERT(3, "高手过招", new int[]{0, 4, 6});

    // 倾向编码
    private final Integer code;

    // 倾向描述
    private final String desc;

    // 简单、中等、困难的配比
    private final int[] ratio;

    // 根据编码获取倾向，不存在返回 null
    public static ExamAiTendency getByCode(Integer code) {
        for (ExamAiTendency item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
