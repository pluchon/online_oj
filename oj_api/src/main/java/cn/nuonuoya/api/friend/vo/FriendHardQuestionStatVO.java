package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;

// 难题分析中的单题统计（计数字段见 FriendSubmitStatBaseVO；用例与申诉计数用于出题质量提醒）
@Getter
@Setter
@ToString(callSuper = true)
public class FriendHardQuestionStatVO extends FriendSubmitStatBaseVO {

    @Serial
    private static final long serialVersionUID = 1L;

    // 题目ID
    private Long questionId;

    // 记录了首个未通过用例的失败提交数（编译错误等没跑到用例的不计）
    private Integer caseFailCount = 0;

    // 失败最集中的用例ID（没有记录时为空）
    private Long topCaseId;

    // 首个未通过用例是该用例的失败提交数
    private Integer topCaseFailCount = 0;

    // 申诉成立（改判通过）的次数
    private Integer upheldAppealCount = 0;
}
