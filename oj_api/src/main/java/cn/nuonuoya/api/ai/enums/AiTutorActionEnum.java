package cn.nuonuoya.api.ai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 做题辅导的提问类型（快捷操作的文案同时作为用户消息展示）
@Getter
@AllArgsConstructor
public enum AiTutorActionEnum {

    // 自由提问
    CHAT(0, "自由提问"),

    // 给一点解题思路
    HINT(1, "指点迷津"),

    // 分析最近一次未通过的提交
    ANALYZE_SUBMIT(2, "分析我最近一次提交"),

    // 解释最近一次提交的编译错误
    EXPLAIN_COMPILE(3, "解释编译错误"),

    // 点评最近一次通过的提交
    REVIEW_CODE(4, "点评我的代码");

    // 类型编码
    private final Integer code;

    // 快捷操作文案
    private final String label;

    // 最小编码（供请求参数校验）
    public static final int MIN_CODE = 0;

    // 最大编码（供请求参数校验，新增类型时同步调整）
    public static final int MAX_CODE = 4;

    // 根据编码获取类型，不存在返回 null
    public static AiTutorActionEnum getByCode(Integer code) {
        for (AiTutorActionEnum action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        return null;
    }
}
