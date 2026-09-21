package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

// 竞赛题目绑定请求参数DTO
@Getter
@Setter
@ToString
@Schema(description = "竞赛题目绑定请求参数")
public class ExamQuestionAddDTO {

    // 竞赛ID
    @Schema(description = "竞赛ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long examId;

    // 选中的题目ID列表
    @Schema(description = "题目ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请至少选择一道题目")
    private List<Long> questionIds;
}
