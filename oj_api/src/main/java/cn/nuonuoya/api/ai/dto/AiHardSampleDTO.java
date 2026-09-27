package cn.nuonuoya.api.ai.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 难题分析中抽查的一份未通过代码
@Getter
@Setter
@ToString
public class AiHardSampleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 学员代码
    private String userCode;

    // 首个未通过用例的实际输出（未记录时为空）
    private String actualOutput;
}
