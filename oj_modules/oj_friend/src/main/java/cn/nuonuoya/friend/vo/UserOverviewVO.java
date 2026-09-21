package cn.nuonuoya.friend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

// C端学员数据总览视图对象
@Getter
@Setter
@ToString
@Schema(description = "C端学员数据总览统计响应")
public class UserOverviewVO implements Serializable {

    // 已解决题目数
    @Schema(description = "已解决题目数")
    private Integer solvedCount;

    // 尝试中题目数
    @Schema(description = "尝试中题目数")
    private Integer tryingCount;

    // 总提交次数
    @Schema(description = "总提交次数")
    private Integer submitCount;

    // 通过率（如 "83%"）
    @Schema(description = "通过率")
    private String passRate;

    // 五维能力雷达图分值
    @Schema(description = "五维能力雷达图分值")
    private UserAbilityRadarVO radarScores;

    // 五维能力模型评分对象（兼容历史字段）
    @Schema(description = "五维能力模型评分对象（兼容历史字段）")
    private UserAbilityRadarVO abilityRadar;

    public void setRadarScores(UserAbilityRadarVO radarScores) {
        this.radarScores = radarScores;
        if (this.abilityRadar == null) {
            this.abilityRadar = radarScores;
        }
    }

    public void setAbilityRadar(UserAbilityRadarVO abilityRadar) {
        this.abilityRadar = abilityRadar;
        if (this.radarScores == null) {
            this.radarScores = abilityRadar;
        }
    }
}
