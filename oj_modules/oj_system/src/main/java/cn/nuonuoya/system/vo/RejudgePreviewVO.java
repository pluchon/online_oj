package cn.nuonuoya.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 按题重判影响范围视图对象
@Getter
@Setter
public class RejudgePreviewVO {

    // 本次会重判的提交数
    @Schema(description = "本次会重判的提交数（练习 + 未结算竞赛）")
    private Integer rejudgeCount;

    // 其中练习提交数
    @Schema(description = "其中练习提交数")
    private Integer practiceCount;

    // 涉及的未结算竞赛
    @Schema(description = "涉及的未结算竞赛")
    private List<RejudgeExamVO> exams;

    // 跳过的已结算竞赛提交数
    @Schema(description = "跳过的已结算竞赛提交数")
    private Integer settledCount;

    // 跳过的评测中提交数
    @Schema(description = "跳过的评测中提交数")
    private Integer judgingCount;
}
