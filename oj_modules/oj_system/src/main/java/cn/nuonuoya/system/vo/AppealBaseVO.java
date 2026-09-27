package cn.nuonuoya.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 申诉列表与详情共用的视图字段
@Getter
@Setter
public class AppealBaseVO {

    // 申诉ID
    @Schema(description = "申诉ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long appealId;

    // 被申诉的提交ID
    @Schema(description = "被申诉的提交ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitId;

    // 申诉人ID
    @Schema(description = "申诉人ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    // 申诉人昵称
    @Schema(description = "申诉人昵称")
    private String nickName;

    // 题目ID
    @Schema(description = "题目ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    // 题目名称
    @Schema(description = "题目名称（题目已删除时为空）")
    private String questionTitle;

    // 竞赛ID
    @Schema(description = "竞赛ID（练习提交为空）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    // 申诉状态
    @Schema(description = "申诉状态 0:待处理 1:存疑 2:通过 3:不通过")
    private Integer status;

    // 申诉状态描述
    @Schema(description = "申诉状态描述")
    private String statusDesc;

    // 申诉时的判题结论
    @Schema(description = "申诉时的判题结论（见 JudgeStatusEnum）")
    private Integer originJudgeStatus;

    // 申诉时的判题结论描述
    @Schema(description = "申诉时的判题结论描述")
    private String originJudgeStatusDesc;

    // 申诉时间
    @Schema(description = "申诉时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 裁定时间
    @Schema(description = "裁定时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
}
