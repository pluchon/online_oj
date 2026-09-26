package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

// 提交记录列表项（不含代码）
@Getter
@Setter
@ToString
public class FriendSubmitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 提交记录ID
    private Long submitId;

    // 用户ID
    private Long userId;

    // 题目ID
    private Long questionId;

    // 竞赛ID（为空表示练习提交）
    private Long examId;

    // 语言类型（见 ProgramTypeEnum）
    private Integer programType;

    // 是否通过（0: 未通过 1: 通过 2: 评测中）
    private Integer pass;

    // 判题结论（见 JudgeStatusEnum，评测中为空）
    private Integer judgeStatus;

    // 得分
    private Integer score;

    // 通过用例数
    private Integer passCount;

    // 总用例数
    private Integer totalCount;

    // 执行耗时（毫秒）
    private Integer timeCost;

    // 提交时间
    private LocalDateTime createTime;

    // 最近一次判题回写或重判时间
    private LocalDateTime updateTime;
}
