package cn.nuonuoya.api.ai.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.util.List;

// 申诉 AI 初审请求（题面见 AiQuestionBaseDTO；学员代码、判题结论与失败用例）
@Getter
@Setter
@ToString(callSuper = true)
public class AiAppealReviewDTO extends AiQuestionBaseDTO {

    @Serial
    private static final long serialVersionUID = 1L;

    // 官方题解（参考解法，可为空）
    private String editorial;

    // 学员代码
    private String userCode;

    // 判题结论描述（如答案错误、运行超时）
    private String verdict;

    // 通过用例数
    private Integer passCount;

    // 总用例数
    private Integer totalCount;

    // 执行回显（编译错误、运行异常等）
    private String exeMessage;

    // 未通过的用例（按用例顺序，最多若干条）
    private List<AiAppealCaseDTO> failedCases;
}
