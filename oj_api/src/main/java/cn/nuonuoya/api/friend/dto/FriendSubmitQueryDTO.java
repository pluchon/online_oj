package cn.nuonuoya.api.friend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 提交记录分页查询条件（条件为空表示不限）
@Getter
@Setter
@ToString
public class FriendSubmitQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 当前页码
    private Integer pageNum;

    // 每页条数
    private Integer pageSize;

    // 题目ID
    private Long questionId;

    // 用户ID范围（调用方按昵称解析出的用户，传空列表表示没有匹配的用户）
    private List<Long> userIds;

    // 判题结论（见 JudgeStatusEnum）
    private Integer judgeStatus;

    // 只看评测中的提交
    private Boolean judging;

    // 竞赛ID
    private Long examId;

    // 只看练习提交（不属于任何竞赛）
    private Boolean practiceOnly;
}
