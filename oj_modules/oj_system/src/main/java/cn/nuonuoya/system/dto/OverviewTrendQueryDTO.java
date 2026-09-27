package cn.nuonuoya.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 提交趋势查询DTO
@Getter
@Setter
@ToString
@Schema(description = "提交趋势查询参数")
public class OverviewTrendQueryDTO {

    // 最近多少天（含今日）
    @NotNull(message = "统计天数不能为空")
    @Min(value = 1, message = "统计天数须在 1 ~ 30 之间")
    @Max(value = 30, message = "统计天数须在 1 ~ 30 之间")
    @Schema(description = "最近多少天（含今日，1 ~ 30）")
    private Integer days;
}
