package cn.nuonuoya.api.ai.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 申诉初审中的一条未通过用例
@Getter
@Setter
@ToString
public class AiAppealCaseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 用例序号（从 1 开始）
    private Integer index;

    // 是否公开示例
    private Boolean sample;

    // 判题输入
    private String input;

    // 预期输出
    private String expectedOutput;

    // 学员程序的实际输出（未记录时为空）
    private String actualOutput;
}
