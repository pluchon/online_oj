package cn.nuonuoya.api.ai.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 被分析的提交记录（隐藏用例只给序号，不给输入与预期输出）
@Getter
@Setter
@ToString
public class AiTutorSubmissionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 提交的代码
    private String code;

    // 判题结论（中文，如"答案错误"）
    private String verdict;

    // 通过用例数
    private Integer passCount;

    // 总用例数
    private Integer totalCount;

    // 编译错误或运行异常信息
    private String exeMessage;

    // 首个未通过用例的序号（从 1 开始，全部通过时为空）
    private Integer failCaseIndex;

    // 首个未通过用例是否为公开示例
    private Boolean failCaseSample;

    // 首个未通过用例的输入（仅公开示例）
    private String failInput;

    // 首个未通过用例的预期输出（仅公开示例）
    private String failExpected;

    // 首个未通过用例的实际输出（仅公开示例）
    private String failActual;
}
