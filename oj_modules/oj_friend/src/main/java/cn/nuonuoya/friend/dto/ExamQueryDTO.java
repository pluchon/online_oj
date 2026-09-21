package cn.nuonuoya.friend.dto;

import cn.nuonuoya.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// C端竞赛列表分页查询DTO
@Getter
@Setter
@ToString
@Schema(description = "C端竞赛列表分页查询参数")
public class ExamQueryDTO extends PageQuery {

    // C端竞赛默认每页条数
    private static final int DEFAULT_PAGE_SIZE = 8;

    // 竞赛分类类型（0: 未完赛 1: 历史竞赛）
    @Schema(description = "竞赛分类类型（0: 未完赛 1: 历史竞赛）")
    private Integer type;

    // 竞赛标题（支持模糊查询）
    @Schema(description = "竞赛标题（支持模糊查询）")
    private String title;

    // 筛选范围开始时间
    @Schema(description = "筛选范围开始时间")
    private String startTime;

    // 筛选范围结束时间
    @Schema(description = "筛选范围结束时间")
    private String endTime;

    public ExamQueryDTO() {
        setPageSize(DEFAULT_PAGE_SIZE);
    }

    // 设置每页条数（空或非法时使用本接口默认条数，上限由父类控制）
    @Override
    public void setPageSize(Integer pageSize) {
        super.setPageSize(pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize);
    }
}
