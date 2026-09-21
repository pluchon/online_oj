package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 题目测试用例类型枚举
@Getter
public enum QuestionCaseTypeEnum {

    // 隐藏用例，仅提交评测时使用，不对学员展示
    HIDDEN(0, "隐藏用例"),

    // 公开示例，题面展示并用于运行
    SAMPLE(1, "公开示例");

    // 类型编码
    private final Integer code;

    // 类型描述
    private final String desc;

    QuestionCaseTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
