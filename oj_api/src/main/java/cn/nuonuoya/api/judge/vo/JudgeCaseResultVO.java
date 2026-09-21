package cn.nuonuoya.api.judge.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 判题单个测试用例执行结果值对象
@Getter
@Setter
@ToString
public class JudgeCaseResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 用例ID
    private Long caseId;

    // 实际输出（程序未执行到该用例时为空）
    private String actualOutput;

    // 是否通过
    private Boolean pass;
}
