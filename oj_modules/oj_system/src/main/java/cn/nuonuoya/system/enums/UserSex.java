package cn.nuonuoya.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 用户性别枚举
@AllArgsConstructor
@Getter
public enum UserSex {

    // 保密
    SECRET(0, "保密"),
    // 男
    MALE(1, "男"),
    // 女
    FEMALE(2, "女");

    // 性别数值
    private final int value;

    // 性别描述
    private final String desc;

    // 根据数值获取性别描述
    public static String getDescByValue(Integer value) {
        if (value == null) {
            return SECRET.getDesc();
        }
        for (UserSex item : values()) {
            if (item.getValue() == value) {
                return item.getDesc();
            }
        }
        return SECRET.getDesc();
    }
}
