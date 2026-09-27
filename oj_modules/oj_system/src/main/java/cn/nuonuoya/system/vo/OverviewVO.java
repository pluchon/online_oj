package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 数据概览视图对象
@Getter
@Setter
public class OverviewVO {

    // 今日统计
    @Schema(description = "今日统计")
    private OverviewPeriodVO today;

    // 近 7 天统计
    @Schema(description = "近 7 天统计（含今日）")
    private OverviewPeriodVO week;

    // 难题榜
    @Schema(description = "难题榜：已出结论提交满 5 条的题中通过率最低的 5 道")
    private List<OverviewQuestionVO> hardQuestions;
}
