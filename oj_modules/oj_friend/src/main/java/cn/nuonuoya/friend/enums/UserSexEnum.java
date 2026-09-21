package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 用户性别枚举
@Getter
public enum UserSexEnum {

    // 保密
    SECRET(0, "保密"),

    // 男
    MALE(1, "男"),

    // 女
    FEMALE(2, "女");

    // 性别编码
    private final Integer code;

    // 性别描述
    private final String desc;

    UserSexEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据性别编码获取描述，未匹配返回"保密"
    public static String getDescByCode(Integer code) {
        if (code == null) {
            return SECRET.getDesc();
        }
        for (UserSexEnum sex : values()) {
            if (sex.getCode().equals(code)) {
                return sex.getDesc();
            }
        }
        return SECRET.getDesc();
    }
}
