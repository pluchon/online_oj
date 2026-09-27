package cn.nuonuoya.api.friend.enums;

import lombok.Getter;

// 申诉状态（契约枚举：oj-friend 存储，oj-system 裁定与展示）
@Getter
public enum AppealStatusEnum {

    // 待处理
    PENDING(0, "待处理"),

    // 存疑（暂不裁定，保留待处理）
    DOUBTFUL(1, "存疑"),

    // 通过（申诉成立，提交改判为通过）
    UPHELD(2, "通过"),

    // 不通过（驳回，维持原结论）
    REJECTED(3, "不通过");

    // 状态编码
    private final Integer code;

    // 状态描述
    private final String desc;

    AppealStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据编码获取枚举，未匹配返回 null
    public static AppealStatusEnum getByCode(Integer code) {
        for (AppealStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    // 是否已给出结论（通过、不通过；管理员仍可改判）
    public boolean isFinal() {
        return this == UPHELD || this == REJECTED;
    }
}
