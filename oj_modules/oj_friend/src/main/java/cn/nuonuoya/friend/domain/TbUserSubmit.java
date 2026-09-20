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
}
