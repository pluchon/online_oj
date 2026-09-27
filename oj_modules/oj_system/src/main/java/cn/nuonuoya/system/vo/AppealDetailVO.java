package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// 申诉详情视图对象（公共字段见 AppealBaseVO，另含申诉理由、AI 分析、提交代码与逐用例结果）
@Getter
@Setter
public class AppealDetailVO extends AppealBaseVO {

    // 申诉理由
    @Schema(description = "申诉理由")
    private String reason;

    // AI 初审分析
    @Schema(description = "AI 初审分析（可能涉及隐藏用例，只给管理员看）")
    private String aiAnalysis;

    // 提交代码
    @Schema(description = "提交代码")
    private String userCode;

    // 语言类型
    @Schema(description = "语言类型（0: Java）")
    private Integer programType;

    // 当前判题结论
    @Schema(description = "当前判题结论（申诉通过后为运行通过）")
    private Integer judgeStatus;

    // 通过用例数
    @Schema(description = "通过用例数")
    private Integer passCount;

    // 总用例数
    @Schema(description = "总用例数")
    private Integer totalCount;

    // 执行耗时（毫秒）
    @Schema(description = "执行耗时（毫秒）")
    private Integer timeCost;

    // 执行回显
    @Schema(description = "执行回显（编译错误、运行异常等）")
    private String exeMessage;

    // 提交时间
    @Schema(description = "提交时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    // 逐用例结果
    @Schema(description = "逐用例结果（按判题时的用例顺序）")
    private List<AppealCaseVO> cases;
}
