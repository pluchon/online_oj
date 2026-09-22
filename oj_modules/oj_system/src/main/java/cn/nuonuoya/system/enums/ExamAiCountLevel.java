package cn.nuonuoya.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// AI 帮建竞赛的题目数量档位
@Getter
@AllArgsConstructor
public enum ExamAiCountLevel {

    // 少量
    FEW(1, "少量", 1, 5),

    // 适中
    MODERATE(2, "适中", 6, 10),

    // 偏多
    MANY(3, "偏多", 11, 15),

    // 超多
    HUGE(4, "超多", 16, 30);

    // 档位编码
    private final Integer code;

    // 档位描述
    private final String desc;

    // 最少题数
    private final int min;

    // 最多题数
    private final int max;

    // 根据编码获取档位，不存在返回 null
    public static ExamAiCountLevel getByCode(Integer code) {
        for (ExamAiCountLevel item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    // 确定题数：描述中给出的数量在区间内时采用，否则取区间中间值
    public int resolve(Integer requested) {
        if (requested != null && requested >= min && requested <= max) {
            return requested;
        }
        return (min + max + 1) / 2;
    }
}
