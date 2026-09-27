package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 单个用例的判题结果（未通过的用例才有实际输出）
@Getter
@Setter
@ToString
public class FriendCaseResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 用例ID
    private Long caseId;

    // 是否通过（未执行为空）
    private Boolean pass;

    // 实际输出（通过的用例不记录）
    private String output;
}
