package cn.nuonuoya.api.judge.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 判题单个测试用例数据传输对象
@Getter
@Setter
@ToString
public class JudgeCaseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 用例ID
    private Long caseId;

    // 判题用输入（按 main 函数约定逐行给出参数）
    private String input;

    // 判题用预期输出（单行）
    private String expectedOutput;
}
