package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 提交记录通过状态枚举（0、1 与判题契约 JudgePassEnum 一致，2 为本服务的评测中状态）
@Getter
public enum SubmitPassEnum {

    // 未通过
    NOT_PASS(0, "未通过"),

    // 通过
    PASS(1, "通过"),

    // 评测中（已投递判题，尚未回写结果）
    JUDGING(2, "评测中");

    // 状态编码
    private final Integer code;

    // 状态描述
    private final String desc;

    SubmitPassEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
