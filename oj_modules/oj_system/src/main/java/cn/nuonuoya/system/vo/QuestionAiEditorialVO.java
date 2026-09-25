package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// AI 题解草稿（回填题解编辑框，随题目保存）
@Getter
@Setter
@Schema(description = "AI 题解草稿")
public class QuestionAiEditorialVO {

    // 题解内容（Markdown）
    @Schema(description = "题解内容（Markdown）")
    private String content;
}
