package cn.nuonuoya.api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 发给 AI 的题面公共字段（做题辅导、申诉初审共用）
@Getter
@Setter
@ToString
public class AiQuestionBaseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 题目标题
    @NotBlank(message = "题目标题不能为空")
    private String questionTitle;

    // 题目描述
    @NotBlank(message = "题目描述不能为空")
    private String questionContent;

    // 需要实现的方法（默认代码块）
    private String defaultCode;
}
