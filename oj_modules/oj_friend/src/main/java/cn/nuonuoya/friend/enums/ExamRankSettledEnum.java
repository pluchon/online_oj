package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 竞赛排名结算状态枚举
@Getter
public enum ExamRankSettledEnum {

    // 未结算
    UNSETTLED(0, "未结算"),

    // 已结算（排名已落库、战报已发送）
    SETTLED(1, "已结算");

    // 状态编码
    private final Integer code;

    // 状态描述
    private final String desc;

    ExamRankSettledEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
