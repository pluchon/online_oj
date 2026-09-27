package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 赛后复盘视图对象（数字来自提交统计，summary 与 comment 为 AI 结论）
@Getter
@Setter
public class ExamReviewVO {

    // 竞赛ID
    @Schema(description = "竞赛ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    // 竞赛名称
    @Schema(description = "竞赛名称")
    private String examTitle;

    // 得分
    @Schema(description = "得分")
    private Integer score;

    // 排名
    @Schema(description = "排名")
    private Integer examRank;

    // 参赛人数
    @Schema(description = "参赛人数（报名人数，与排名榜一致）")
    private Integer participantCount = 0;

    // 题目数
    @Schema(description = "题目数")
    private Integer questionCount = 0;

    // 通过题数
    @Schema(description = "通过题数")
    private Integer passedCount = 0;

    // 剩余重新生成次数
    @Schema(description = "这场还能手动重新生成的次数")
    private Integer regenerateRemaining;

    // 生成时间
    @Schema(description = "生成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedTime;

    // 整体总结
    @Schema(description = "AI 整体总结")
    private String summary;

    // 逐题回顾
    @Schema(description = "逐题回顾（按竞赛题目顺序）")
    private List<ExamReviewQuestionVO> questions = new ArrayList<>();
}
