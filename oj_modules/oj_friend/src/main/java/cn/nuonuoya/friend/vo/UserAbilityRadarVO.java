package cn.nuonuoya.friend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

// C端学员五维能力模型雷达图VO
@Getter
@Setter
@ToString
@Schema(description = "五维能力模型评分对象")
public class UserAbilityRadarVO implements Serializable {

    // 数据结构能力评分 (0~100)
    @Schema(description = "数据结构能力评分 (0~100)")
    private Integer dataStructure;

    // 算法思维能力评分 (0~100)
    @Schema(description = "算法思维能力评分 (0~100)")
    private Integer algorithm;

    // 工程实现能力评分 (0~100)
    @Schema(description = "工程实现能力评分 (0~100)")
    private Integer implementation;

    // 数学逻辑能力评分 (0~100)
    @Schema(description = "数学逻辑能力评分 (0~100)")
    private Integer math;

    // 竞赛实战能力评分 (0~100)
    @Schema(description = "竞赛实战能力评分 (0~100)")
    private Integer competition;
}
