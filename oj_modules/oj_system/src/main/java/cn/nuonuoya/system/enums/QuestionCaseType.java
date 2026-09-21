package cn.nuonuoya.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 题目测试用例类型枚举
@AllArgsConstructor
@Getter
public enum QuestionCaseType {

    // 隐藏用例，仅提交评测时使用
    HIDDEN(0, "隐藏用例"),

    // 公开示例，题面展示并用于运行
    SAMPLE(1, "公开示例");

    // 类型数值
    private final int value;

    // 类型描述
    private final String desc;
}
