package cn.nuonuoya.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// C端学员数据总览查询DTO
@Getter
@Setter
@ToString
@Schema(description = "C端学员数据总览查询入参")
public class UserOverviewQueryDTO {

    // 时间范围筛选：all-全部时间, year-近一年, month-近一月, week-本周
    @Schema(description = "时间范围筛选：all-全部时间, year-近一年, month-近一月, week-本周", defaultValue = "all")
    private String timeRange = "all";
}
