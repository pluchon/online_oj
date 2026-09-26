package cn.nuonuoya.system.dto;

import cn.nuonuoya.common.domain.PageQuery;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 提交记录分页查询DTO（条件为空表示不限）
@Getter
@Setter
@ToString
public class SubmitQueryDTO extends PageQuery {

    // 题目ID
    private Long questionId;

    // 用户昵称（模糊查询）
    private String nickName;

    // 判题结论（见 JudgeStatusEnum）
    private Integer judgeStatus;

    // 只看评测中的提交
    private Boolean judging;

    // 竞赛ID
    private Long examId;

    // 只看练习提交
    private Boolean practiceOnly;
}
