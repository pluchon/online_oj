package cn.nuonuoya.system.dto;

import cn.nuonuoya.system.enums.OverviewTrendRange;
import io.swagger.v3.oas.annotations.media.Schema;
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

    // 时间范围
    @NotNull(message = "统计范围不能为空")
    @Schema(description = "时间范围：WEEK 近七天、TWO_WEEKS 近十四天、MONTH 近一个月（按天），HALF_YEAR 近半年（按周），YEAR 近一年（按半月）")
    private OverviewTrendRange range;
}
