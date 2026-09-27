package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 难题分析视图对象（数字来自提交统计，summary 与 comment 为 AI 结论；数据不够时只有范围字段）
@Getter
@Setter
public class OverviewHardAnalysisVO {

    // 数据是否足够（不够时不调用 AI，其余结论字段为空）
    @Schema(description = "数据是否足够")
    private Boolean sufficient = false;

    // 分析所需的最少题数
    @Schema(description = "分析所需的最少题数（每题已出结论的提交满 5 条）")
    private Integer minQuestionCount;

    // 参与分析的题数
    @Schema(description = "参与分析的题数（已出结论的提交满 5 条）")
    private Integer questionCount = 0;

    // 这些题已出结论的提交数
    @Schema(description = "这些题已出结论的提交数")
    private Integer judgedCount = 0;

    // 生成时间
    @Schema(description = "生成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedTime;

    // 出题质量提醒
    @Schema(description = "出题质量提醒：失败集中在单个用例或有成立申诉的题")
    private List<OverviewHardSuspectVO> suspects = new ArrayList<>();

    // 整体薄弱点结论
    @Schema(description = "整体薄弱点的 AI 结论")
    private String weakSummary;

    // 通过率最低的标签
    @Schema(description = "通过率最低的若干个标签（升序）")
    private List<OverviewTagStatVO> weakTags = new ArrayList<>();

    // 通过率最高的标签
    @Schema(description = "通过率最高的若干个标签（降序，与最低的不重复）")
    private List<OverviewTagStatVO> strongTags = new ArrayList<>();

    // 主要错误类型结论
    @Schema(description = "主要错误类型的 AI 结论")
    private String verdictSummary;

    // 判题结论分布
    @Schema(description = "未通过提交的判题结论分布（降序）")
    private List<OverviewVerdictVO> verdicts = new ArrayList<>();
}
