package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

// 申诉列表与详情共用的字段
@Getter
@Setter
@ToString
public class FriendAppealBaseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 申诉ID
    private Long appealId;

    // 被申诉的提交ID
    private Long submitId;

    // 申诉人ID
    private Long userId;

    // 题目ID
    private Long questionId;

    // 竞赛ID（练习提交为空）
    private Long examId;

    // 申诉状态（见 AppealStatusEnum）
    private Integer status;

    // 申诉时的判题结论（见 JudgeStatusEnum）
    private Integer originJudgeStatus;

    // 申诉时间
    private LocalDateTime createTime;

    // 裁定时间
    private LocalDateTime handleTime;
}
