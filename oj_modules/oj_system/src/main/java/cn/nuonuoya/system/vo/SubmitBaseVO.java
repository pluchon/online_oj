package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 提交记录列表与详情共用的视图字段
@Getter
@Setter
public class SubmitBaseVO {

    // 提交记录ID
    @Schema(description = "提交记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitId;

    // 用户ID
    @Schema(description = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    // 用户昵称
    @Schema(description = "用户昵称（用户不存在时为空）")
    private String nickName;

    // 题目ID
    @Schema(description = "题目ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    // 题目标题
    @Schema(description = "题目标题（题目已删除时为空）")
    private String questionTitle;

    // 竞赛ID
    @Schema(description = "竞赛ID（为空表示练习提交）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    // 竞赛标题
    @Schema(description = "竞赛标题（练习提交或竞赛已删除时为空）")
    private String examTitle;

    // 语言类型
    @Schema(description = "语言类型（0: Java）")
    private Integer programType;

    // 是否通过
    @Schema(description = "是否通过（0: 未通过 1: 通过 2: 评测中）")
    private Integer pass;

    // 判题结论
    @Schema(description = "判题结论（见 JudgeStatusEnum，评测中为空）")
    private Integer judgeStatus;

    // 判题结论描述
    @Schema(description = "判题结论描述")
    private String judgeStatusDesc;

    // 得分
    @Schema(description = "得分")
    private Integer score;

    // 通过用例数
    @Schema(description = "通过用例数")
    private Integer passCount;

    // 总用例数
    @Schema(description = "总用例数")
    private Integer totalCount;

    // 执行耗时（毫秒）
    @Schema(description = "执行耗时（毫秒）")
    private Integer timeCost;

    // 提交时间
    @Schema(description = "提交时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 最近一次判题回写或重判时间
    @Schema(description = "最近一次判题回写或重判时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
