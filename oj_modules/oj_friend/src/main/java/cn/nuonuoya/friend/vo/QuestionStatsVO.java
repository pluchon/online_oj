package cn.nuonuoya.friend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

// 题库解题统计视图对象
@Getter
@Setter
@Schema(description = "题库解题统计视图对象")
public class QuestionStatsVO implements Serializable {

    // 题目总数量
    @Schema(description = "题目总数量")
    private Long totalCount;

    // 当前用户已攻克题目数量
    @Schema(description = "当前用户已攻克题目数量")
    private Long solvedCount;

    // 当前用户正在尝试中题目数量
    @Schema(description = "当前用户正在尝试中题目数量")
    private Long inProgressCount;
}
