package cn.nuonuoya.system.dto;

import cn.nuonuoya.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 竞赛列表分页查询DTO
@Getter
@Setter
@ToString
@Schema(description = "竞赛列表分页查询参数")
public class ExamDTO extends PageQuery {

    // 竞赛标题（支持模糊查询）
    @Schema(description = "竞赛标题（支持模糊查询）")
    private String title;

    // 筛选范围开始时间
    @Schema(description = "筛选范围开始时间")
    private String startTime;

    // 筛选范围结束时间
    @Schema(description = "筛选范围结束时间")
    private String endTime;
}
