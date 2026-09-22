package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// AI 解法示例（只展示，不保存）
@Getter
@Setter
@Schema(description = "AI 解法示例")
public class QuestionAiSolutionVO {

    // 解法代码
    @Schema(description = "解法代码")
    private String code;
}
