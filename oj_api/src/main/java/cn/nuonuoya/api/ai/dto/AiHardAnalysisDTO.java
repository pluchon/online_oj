package cn.nuonuoya.api.ai.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// 难题分析请求：统计范围、按标签与按判题结论的数字，以及需要判断出题质量的可疑题
@Getter
@Setter
@ToString
public class AiHardAnalysisDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 参与分析的题数
    private Integer questionCount;

    // 这些题已出结论的提交数
    private Integer judgedCount;

    // 通过率最低的标签（升序）
    private List<AiHardTagDTO> weakTags = new ArrayList<>();

    // 通过率最高的标签（降序）
    private List<AiHardTagDTO> strongTags = new ArrayList<>();

    // 未通过提交的判题结论分布（按数量降序）
    private List<AiHardVerdictDTO> verdicts = new ArrayList<>();

    // 可疑题（失败集中在单个用例或有成立的申诉）
    @Valid
    private List<AiHardSuspectDTO> suspects = new ArrayList<>();
}
