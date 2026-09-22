package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// AI 解法示例请求参数
@Getter
@Setter
@Schema(description = "AI 解法示例请求参数")
public class QuestionAiSolutionDTO {

    // 题目标题
    @Schema(description = "题目标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "题目标题不能为空")
    @Size(max = 50, message = "题目标题长度不能超过50个字符")
    private String title;

    // 题目描述
    @Schema(description = "题目描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "题目内容描述不能为空")
    @Size(max = 1000, message = "题目内容描述长度不能超过1000个字符")
    private String content;

    // 默认代码块
    @Schema(description = "默认代码块", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "默认代码块不能为空")
    @Size(max = 500, message = "默认代码块长度不能超过500个字符")
    private String defaultCode;
}
