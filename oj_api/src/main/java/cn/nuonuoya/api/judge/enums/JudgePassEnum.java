package cn.nuonuoya.api.judge.enums;

import lombok.Getter;

// 判题是否通过枚举（对应 JudgeResultVO.pass）
@Getter
public enum JudgePassEnum {

    // 未通过
    NOT_PASS(0, "未通过"),

    // 通过
    PASS(1, "通过");

    // 结果编码
    private final Integer code;

    // 结果描述
    private final String desc;

    JudgePassEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
