package cn.nuonuoya.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 用户状态枚举
@AllArgsConstructor
@Getter
public enum UserStatus {

    // 拉黑
    BANNED(0, "拉黑"),
    // 正常
    NORMAL(1, "正常");

    // 状态数值
    private final int value;

    // 状态描述
    private final String desc;

    // 根据数值获取状态描述
    public static String getDescByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (UserStatus item : values()) {
            if (item.getValue() == value) {
                return item.getDesc();
            }
        }
        return null;
    }
}
