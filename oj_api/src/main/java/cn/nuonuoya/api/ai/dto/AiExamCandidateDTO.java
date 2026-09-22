package cn.nuonuoya.api.ai.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 候选题目（发给模型挑选时用序号指代，避免模型改写长整型题号）
@Getter
@Setter
@ToString
public class AiExamCandidateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 候选序号（从 1 开始）
    private Integer index;

    // 题目标题
    private String title;

    // 难度（1:简单 2:中等 3:困难）
    private Integer difficulty;

    // 描述摘要
    private String summary;

    // 通过率（0~1，提交数不足时为空）
    private Double passRate;
}
