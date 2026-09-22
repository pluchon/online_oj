package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// AI 帮建竞赛请求参数
@Getter
@Setter
@Schema(description = "AI 帮建竞赛请求参数")
public class ExamAiPlanDTO {

    // 一句话描述
    @Schema(description = "一句话描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "描述不能为空")
    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;

    // 难度倾向（1:新手友好 2:一般大众 3:高手过招）
    @Schema(description = "难度倾向（1:新手友好 2:一般大众 3:高手过招）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择难度倾向")
    @Min(value = 1, message = "难度倾向不合法")
    @Max(value = 3, message = "难度倾向不合法")
    private Integer tendency;

    // 题目数量档位（1:少量 2:适中 3:偏多 4:超多）
    @Schema(description = "题目数量档位（1:少量1~5 2:适中6~10 3:偏多11~15 4:超多16~30）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择题目数量")
    @Min(value = 1, message = "题目数量不合法")
    @Max(value = 4, message = "题目数量不合法")
    private Integer countLevel;
}
