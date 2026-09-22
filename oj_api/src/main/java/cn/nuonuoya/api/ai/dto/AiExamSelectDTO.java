package cn.nuonuoya.api.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 竞赛选题请求：从候选中按各难度数量挑题
@Getter
@Setter
@ToString
public class AiExamSelectDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 管理员的一句话描述
    @NotBlank(message = "描述不能为空")
    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;

    // 需要的简单题数量
    @NotNull(message = "简单题数量不能为空")
    private Integer easyCount;

    // 需要的中等题数量
    @NotNull(message = "中等题数量不能为空")
    private Integer mediumCount;

    // 需要的困难题数量
    @NotNull(message = "困难题数量不能为空")
    private Integer hardCount;

    // 候选题目
    @Valid
    @NotEmpty(message = "候选题目不能为空")
    @Size(max = 120, message = "候选题目不能超过120道")
    private List<AiExamCandidateDTO> candidates;
}
