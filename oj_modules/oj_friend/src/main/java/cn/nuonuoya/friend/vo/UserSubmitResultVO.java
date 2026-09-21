package cn.nuonuoya.friend.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 代码提交与判题结果视图对象
@Getter
@Setter
@Schema(description = "代码提交与判题结果视图对象")
public class UserSubmitResultVO {

    // 提交记录ID (字符串防JS精度丢失)
    @Schema(description = "提交记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitId;

    // 题目ID
    @Schema(description = "题目ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    // 竞赛ID
    @Schema(description = "竞赛ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    // 编程语言类型
    @Schema(description = "编程语言类型 0: java 1: cpp")
    private Integer programType;

    // 是否通过 (0: 未通过 1: 通过)
    @Schema(description = "是否通过 0: 未通过 1: 通过")
    private Integer pass;

    // 执行结果反馈文案
    @Schema(description = "执行结果/报错信息")
    private String exeMessage;

    // 得分
    @Schema(description = "得分")
    private Integer score;

    // 判题状态（1:AC 2:WA 3:TLE 4:MLE 5:CE 6:RE 8:SE，评测中为空）
    @Schema(description = "判题状态 1:AC 2:WA 3:TLE 4:MLE 5:CE 6:RE 8:SE")
    private Integer status;

    // 通过用例数
    @Schema(description = "通过用例数")
    private Integer passCount;

    // 总用例数
    @Schema(description = "总用例数")
    private Integer totalCount;

    // 执行耗时（毫秒）
    @Schema(description = "执行耗时(ms)")
    private Integer timeCost;

    // 首个未通过用例
    @Schema(description = "首个未通过用例")
    private CaseResultVO failCase;

    // 逐用例状态（按用例顺序，1: 通过 0: 未通过 -: 未执行）
    @Schema(description = "逐用例状态 1:通过 0:未通过 -:未执行")
    private String caseStates;

    // 提交时间
    @Schema(description = "提交时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
