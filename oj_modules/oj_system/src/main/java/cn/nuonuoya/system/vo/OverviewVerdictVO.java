package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 难题分析中一种判题结论的未通过提交数与占比
@Getter
@Setter
public class OverviewVerdictVO {

    // 判题状态
    @Schema(description = "判题状态（见判题结论枚举）")
    private Integer judgeStatus;

    // 判题结论描述
    @Schema(description = "判题结论描述")
    private String verdict;

    // 未通过提交数
    @Schema(description = "未通过提交数")
    private Integer failCount = 0;

    // 占比
    @Schema(description = "占全部未通过提交的百分比（一位小数）")
    private Double share;
}
