package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

// 难题分析中一种判题结论的未通过提交数
@Getter
@Setter
@ToString
public class FriendVerdictStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 判题状态（见 JudgeStatusEnum）
    private Integer judgeStatus;

    // 未通过提交数
    private Integer failCount = 0;
}
