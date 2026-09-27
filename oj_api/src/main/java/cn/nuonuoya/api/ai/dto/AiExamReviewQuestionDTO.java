package cn.nuonuoya.api.ai.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;

// 赛后复盘中的一道题（题面见 AiQuestionBaseDTO；学员的统计与需要点评的那次提交）
@Getter
@Setter
@ToString(callSuper = true)
public class AiExamReviewQuestionDTO extends AiQuestionBaseDTO {

    @Serial
    private static final long serialVersionUID = 1L;

    // 序号（从 1 开始，AI 按它返回点评）
    private Integer index;

    // 提交次数（0 表示没有提交）
    private Integer submitCount;

    // 是否通过
    private Boolean passed;

    // 首次通过距开赛的分钟数（未通过时为空）
    private Integer passMinutes;

    // 全场通过率（通过人数 ÷ 提交过的人数，百分比）
    private Double passRate;

    // 需要点评的那次提交的判题结论（未通过时是最后一次提交；通过但错过时是通过前最后一次失败；一次通过时为空）
    private String verdict;

    // 那次提交通过的用例数
    private Integer passCount;

    // 那次提交的总用例数
    private Integer totalCount;

    // 那次提交的学员代码
    private String userCode;

    // 那次提交的执行回显（编译错误、运行异常等）
    private String exeMessage;

    // 首个未通过的用例是否公开示例（只有公开示例才带输入与预期输出）
    private Boolean failCaseSample;

    // 公开示例的输入（隐藏用例不提供）
    private String sampleInput;

    // 公开示例的预期输出（隐藏用例不提供）
    private String sampleOutput;
}
