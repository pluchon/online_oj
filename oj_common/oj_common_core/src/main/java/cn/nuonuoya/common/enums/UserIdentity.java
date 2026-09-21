package cn.nuonuoya.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 登录身份枚举（区分用户端与管理端令牌）
@AllArgsConstructor
@Getter
public enum UserIdentity {

    // 普通用户
    ORDINARY(1, "普通用户"),

    // 管理员
    ADMIN(2, "管理员");

    // 身份编码
    private final int value;

    // 身份描述
    private final String msg;
}
