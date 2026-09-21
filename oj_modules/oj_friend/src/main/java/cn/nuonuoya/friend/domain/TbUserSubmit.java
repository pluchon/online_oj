package cn.nuonuoya.friend.domain;

import cn.nuonuoya.common.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 用户代码提交记录实体
@TableName("tb_user_submit")
@Getter
@Setter
@ToString
public class TbUserSubmit extends BaseEntity {

    // 提交记录id (主键，雪花算法)
    @TableId(value = "SUBMIT_ID", type = IdType.ASSIGN_ID)
    private Long submitId;

    // 用户id
    private Long userId;

    // 题目id
    private Long questionId;

    // 竞赛id (为空表示非竞赛练习提交)
    private Long examId;

    // 代码类型 0: java 1: CPP
    private Integer programType;

    // 用户代码
    private String userCode;

    // 判题结果 0: 未通过 1: 通过
    private Integer pass;

    // 执行结果/报错信息
    private String exeMessage;

    // 得分
    private Integer score;

    // 判题状态（见 JudgeStatusEnum）
    private Integer judgeStatus;

    // 通过用例数
    private Integer passCount;

    // 总用例数
    private Integer totalCount;

    // 执行耗时（毫秒）
    private Integer timeCost;

    // 首个未通过用例ID
    private Long failCaseId;

    // 首个未通过用例的实际输出
    private String failOutput;

    // 逐用例状态（按用例顺序，1: 通过 0: 未通过 -: 未执行）
    private String caseStates;
}
