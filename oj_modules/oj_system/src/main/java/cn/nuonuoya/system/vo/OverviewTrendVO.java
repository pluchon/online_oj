package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// 单日趋势视图对象（计数与通过率见 SubmitStatBaseVO）
@Getter
@Setter
public class OverviewTrendVO extends SubmitStatBaseVO {

    // 日期
    @Schema(description = "日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
}
