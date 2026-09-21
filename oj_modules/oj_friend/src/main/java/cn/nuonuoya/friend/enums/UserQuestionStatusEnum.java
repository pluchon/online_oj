package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 学员题目答题状态枚举
@Getter
public enum UserQuestionStatusEnum {

    // 未尝试（用户尚未提交过代码，或未登录）
    UNTOUCHED(0, "未尝试"),

    // 已攻克（用户已有提交记录且至少一次通过）
    SOLVED(1, "已攻克"),

    // 尝试中（用户提交过但尚未通过）
    IN_PROGRESS(2, "尝试中");

    // 状态编码
    private final Integer code;

    // 状态描述
    private final String desc;

    UserQuestionStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据状态码获取对应枚举实例
    public static UserQuestionStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserQuestionStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
