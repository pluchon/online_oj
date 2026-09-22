package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// AI 题面草稿（回填题目表单，不自动保存）
@Getter
@Setter
@Schema(description = "AI 题面草稿")
public class QuestionAiDraftVO {

    // 题目标题
    @Schema(description = "题目标题")
    private String title;

    // 题目难度
    @Schema(description = "题目难度（1:简单 2:中等 3:困难）")
    private Integer difficulty;

    // 时间限制（毫秒）
    @Schema(description = "时间限制（毫秒）")
    private Integer timeLimit;

    // 空间限制（MB）
    @Schema(description = "空间限制（MB）")
    private Integer spaceLimit;

    // 题目描述
    @Schema(description = "题目描述（Markdown）")
    private String content;

    // 默认代码块
    @Schema(description = "默认代码块")
    private String defaultCode;

    // main函数
    @Schema(description = "main函数")
    private String mainFunc;
}
