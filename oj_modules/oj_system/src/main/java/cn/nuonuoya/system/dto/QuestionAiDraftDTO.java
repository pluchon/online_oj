package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// AI 生成题面草稿请求参数
@Getter
@Setter
@Schema(description = "AI 生成题面草稿请求参数")
public class QuestionAiDraftDTO {

    // 一句话题目描述
    @Schema(description = "一句话题目描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "题目描述不能为空")
    @Size(max = 500, message = "题目描述不能超过500个字符")
    private String description;
}
