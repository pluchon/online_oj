package cn.nuonuoya.friend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// AI 辅导消息角色
@Getter
@AllArgsConstructor
public enum AiChatRoleEnum {

    // 用户提问
    USER(1, "用户"),

    // AI 回复
    ASSISTANT(2, "AI");

    // 角色编码
    private final Integer code;

    // 角色描述
    private final String desc;
}
