package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 出题质量提醒中的一道题
@Getter
@Setter
public class OverviewHardSuspectVO {

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

    // 失败最集中的用例序号
    @Schema(description = "失败最集中的用例序号（按判题顺序从 1 开始；失败不集中时为空）")
    private Integer caseIndex;

    // 该用例占失败的比例
    @Schema(description = "首个未通过用例是该用例的失败占比（百分比，一位小数）")
    private Double caseShare;

    // 申诉成立次数
    @Schema(description = "申诉成立（改判通过）的次数")
    private Integer upheldAppealCount = 0;

    // AI 判断
    @Schema(description = "AI 对这道题的判断")
    private String comment;
}
