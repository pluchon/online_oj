package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// AI 帮建竞赛结果（回填表单，不保存）
@Getter
@Setter
@Schema(description = "AI 帮建竞赛结果")
public class ExamAiPlanVO {

    // 竞赛名称
    @Schema(description = "竞赛名称")
    private String title;

    // 选出的题目（由易到难）
    @Schema(description = "选出的题目")
    private List<ExamAiQuestionVO> questions;

    // 计划题数
    @Schema(description = "计划题数")
    private Integer plannedCount;

    // 提示信息（如题库不足时说明实际题数），没有时为空
    @Schema(description = "提示信息")
    private String message;
}
