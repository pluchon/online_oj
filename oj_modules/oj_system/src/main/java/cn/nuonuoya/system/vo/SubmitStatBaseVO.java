package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 提交计数与通过率公共字段（时间段、每日趋势、难题统计共用）
@Getter
@Setter
public class SubmitStatBaseVO {

    // 提交数
    @Schema(description = "提交数（含评测中）")
    private Integer submitCount;

    // 已出结论的提交数
    @Schema(description = "已出结论的提交数（通过率的分母）")
    private Integer judgedCount;

    // 通过的提交数
    @Schema(description = "通过的提交数")
    private Integer passCount;

    // 通过率
    @Schema(description = "通过率（百分比，保留一位小数；没有已出结论的提交时为空）")
    private Double passRate;
}
