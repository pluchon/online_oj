package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

// 时间段统计视图对象（计数与通过率见 SubmitStatBaseVO）
@Getter
@Setter
public class OverviewPeriodVO extends SubmitStatBaseVO {

    // 活跃用户数
    @Schema(description = "活跃用户数（时间段内有提交的去重用户）")
    private Integer activeUsers;
}
