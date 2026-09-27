package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// 提交申诉实体（每条提交只能申诉一次，由未删除提交ID上的唯一约束保证）
@TableName("tb_submit_appeal")
@Getter
@Setter
@ToString
public class TbSubmitAppeal extends BaseEntity {

    // 申诉ID（雪花算法）
    @TableId(value = "APPEAL_ID", type = IdType.ASSIGN_ID)
    private Long appealId;

    // 被申诉的提交ID
    private Long submitId;

    // 申诉人ID
    private Long userId;

    // 题目ID
    private Long questionId;

    // 竞赛ID（练习提交为空）
    private Long examId;

    // 申诉理由
    private String reason;

    // AI 初审分析（只给管理员看）
    private String aiAnalysis;

    // 申诉时的判题结论（见 JudgeStatusEnum）
    private Integer originJudgeStatus;

    // 申诉状态（见 AppealStatusEnum）
    private Integer status;

    // 裁定人（管理员ID）
    private Long handleBy;

    // 裁定时间
    private LocalDateTime handleTime;

    // 逻辑删除标识（0: 正常 1: 已删除）
    @TableLogic
    private Integer deleteState;
}
