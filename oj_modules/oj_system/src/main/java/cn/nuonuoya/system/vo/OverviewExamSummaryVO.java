package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// 时间段内竞赛统计视图对象（汇总人数 + 竞赛分页列表）
@Getter
@Setter
public class OverviewExamSummaryVO {

    // 报名人数
    @Schema(description = "报名人数（时间段内进行过的竞赛，按用户去重）")
    private Integer enrollCount = 0;

    // 参赛人数
    @Schema(description = "参赛人数（在这些竞赛里交过代码的用户，去重）")
    private Integer participantCount = 0;

    // 参赛率
    @Schema(description = "参赛率（参赛人数 ÷ 报名人数，百分比保留一位小数；没有报名时为空）")
    private Double participationRate;

    // 竞赛总数
    @Schema(description = "时间段内进行过的竞赛总数")
    private Long total = 0L;

    // 当前页竞赛
    @Schema(description = "当前页竞赛（按开始时间倒序）")
    private List<OverviewExamVO> rows = new ArrayList<>();
}
