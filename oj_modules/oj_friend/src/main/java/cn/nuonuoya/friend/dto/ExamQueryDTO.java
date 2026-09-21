package cn.nuonuoya.friend.dto;

import cn.nuonuoya.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Setter;
import lombok.ToString;

// C端竞赛列表分页查询DTO
@Setter
@ToString
@Schema(description = "C端竞赛列表分页查询参数")
public class ExamQueryDTO extends PageQuery {

    // C端竞赛每页固定展示8条
    private static final int DEFAULT_PAGE_SIZE = 8;

    public ExamQueryDTO() {
        setPageSize(DEFAULT_PAGE_SIZE);
    }

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

    public Integer getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    @Override
    public Integer getPageSize() {
        Integer size = super.getPageSize();
        return (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : size;
    }

    @Override
    public void setPageSize(Integer pageSize) {
        super.setPageSize((pageSize == null || pageSize <= 0) ? DEFAULT_PAGE_SIZE : pageSize);
    }
}
