package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 用户账号状态枚举
@Getter
public enum UserStatusEnum {

    // 拉黑
    BANNED(0, "拉黑"),

    // 正常
    NORMAL(1, "正常");

    // 状态编码
    private final Integer code;

    // 状态描述
    private final String desc;

    UserStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
