package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// 趋势中的一个点：一天或一段日期的统计（计数与通过率见 SubmitStatBaseVO）
@Getter
@Setter
public class OverviewTrendVO extends SubmitStatBaseVO {

    // 横轴文案
    @Schema(description = "横轴文案（按天为 MM.dd，按周或半月为 MM.dd-MM.dd）")
    private String label;

    // 起始日期
    @Schema(description = "起始日期（含）")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    // 结束日期
    @Schema(description = "结束日期（含）")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
