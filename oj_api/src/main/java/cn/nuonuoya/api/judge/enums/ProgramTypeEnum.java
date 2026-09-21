package cn.nuonuoya.api.judge.enums;

import lombok.Getter;

// 判题语言类型枚举（对应 JudgeRequestDTO.programType）
@Getter
public enum ProgramTypeEnum {

    // Java
    JAVA(0, "Java");

    // 语言编码
    private final Integer code;

    // 语言名称
    private final String desc;

    ProgramTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
