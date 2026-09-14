package cn.nuonuoya.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 竞赛发布状态枚举
@AllArgsConstructor
@Getter
public enum ExamStatus {

    // 未发布
    UNPUBLISHED(0, "未发布"),
    // 已发布
    PUBLISHED(1, "已发布");

    // 状态值
    private final int value;

    // 状态描述
    private final String desc;

    // 根据数值获取状态描述
    public static String getDescByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ExamStatus item : values()) {
            if (item.getValue() == value) {
                return item.getDesc();
            }
        }
        return null;
    }
}
