package cn.nuonuoya.friend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 竞赛状态统计（竞赛中心为全部已发布竞赛，我的竞赛为当前用户已报名的竞赛）
@Getter
@Setter
@Schema(description = "竞赛状态统计")
public class ExamStatsVO {

    // 竞赛总数
    @Schema(description = "竞赛总数")
    private Long total;

    // 进行中场次
    @Schema(description = "进行中场次")
    private Long ongoing;

    // 未开赛场次
    @Schema(description = "未开赛场次")
    private Long notStarted;

    // 已完赛场次
    @Schema(description = "已完赛场次")
    private Long finished;
}
