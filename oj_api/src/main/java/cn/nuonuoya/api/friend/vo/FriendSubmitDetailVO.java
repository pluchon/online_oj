package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;

// 提交记录详情（公共字段见 FriendSubmitBaseVO，另含代码与判题回显）
@Getter
@Setter
@ToString(callSuper = true)
public class FriendSubmitDetailVO extends FriendSubmitBaseVO {

    @Serial
    private static final long serialVersionUID = 1L;

    // 用户代码
    private String userCode;

    // 执行回显（编译错误、运行异常等）
    private String exeMessage;

    // 逐用例状态（按用例顺序，1: 通过 0: 未通过 -: 未执行）
    private String caseStates;

    // 首个未通过用例ID
    private Long failCaseId;

    // 首个未通过用例的实际输出
    private String failOutput;
}
