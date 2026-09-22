package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 站内消息类型枚举（新增消息来源时在此扩展）
@Getter
public enum MessageTypeEnum {

    // 系统通知
    SYSTEM(1, "系统通知"),

    // 竞赛通知（如赛后排名战报）
    EXAM(2, "竞赛通知");

    // 类型编码
    private final Integer code;

    // 类型名称
    private final String desc;

    MessageTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
