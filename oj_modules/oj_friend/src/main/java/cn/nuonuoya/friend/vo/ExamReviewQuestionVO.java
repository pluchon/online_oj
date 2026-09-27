package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 赛后复盘中的一道题
@Getter
@Setter
public class ExamReviewQuestionVO {

    // 题目ID
    @Schema(description = "题目ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    // 题目标题
    @Schema(description = "题目标题")
    private String title;

    // 题目难度
    @Schema(description = "题目难度（1: 简单 2: 中等 3: 困难）")
    private Integer difficulty;

    // 题目难度描述
    @Schema(description = "题目难度描述")
    private String difficultyDesc;

    // 提交次数
    @Schema(description = "本人提交次数（0 表示没有提交）")
    private Integer submitCount = 0;

    // 是否通过
    @Schema(description = "是否通过")
    private Boolean passed = false;

    // 首次通过用时
    @Schema(description = "首次通过距开赛的分钟数（未通过时为空）")
    private Integer passMinutes;

    // 得分
    @Schema(description = "本题得分（各次提交的最高分）")
    private Integer score = 0;

    // 最后一次判题状态
    @Schema(description = "最后一次提交的判题状态（没有提交时为空）")
    private Integer lastJudgeStatus;

    // 最后一次判题结论描述
    @Schema(description = "最后一次提交的判题结论描述")
    private String lastVerdict;

    // 全场提交过的人数
    @Schema(description = "本场提交过这道题的人数")
    private Integer attemptUsers = 0;

    // 全场通过的人数
    @Schema(description = "本场通过这道题的人数")
    private Integer passUsers = 0;

    // 全场通过率
    @Schema(description = "全场通过率（通过人数 ÷ 提交过的人数，百分比一位小数；没人提交时为空）")
    private Double passRate;

    // AI 点评
    @Schema(description = "AI 点评（没有提交的题为空）")
    private String comment;
}
