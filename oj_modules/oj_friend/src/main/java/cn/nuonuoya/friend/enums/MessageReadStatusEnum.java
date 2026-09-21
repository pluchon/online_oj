package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 站内消息已读状态枚举
@Getter
public enum MessageReadStatusEnum {

    // 未读
    UNREAD(0, "未读"),

    // 已读
    READ(1, "已读");

    // 状态编码
    private final Integer code;

    // 状态描述
    private final String desc;

    MessageReadStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
