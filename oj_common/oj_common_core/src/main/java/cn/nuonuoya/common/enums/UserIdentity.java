package cn.nuonuoya.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserIdentity {

    ORDINARY(1,"普通用户"),
    ADMIN(2,"管理员"),
    ;

    private final int value;

    private final String msg;
}
