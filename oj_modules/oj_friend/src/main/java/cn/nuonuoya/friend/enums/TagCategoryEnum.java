package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 标签分类枚举（能力雷达图按分类统计）
@Getter
public enum TagCategoryEnum {

    // 数据结构
    DATA_STRUCTURE(1, "数据结构"),

    // 算法
    ALGORITHM(2, "算法"),

    // 数学
    MATH(3, "数学"),

    // 其他
    OTHER(4, "其他");

    // 分类编码
    private final Integer code;

    // 分类描述
    private final String desc;

    TagCategoryEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据分类编码获取描述，未匹配返回"其他"
    public static String getDescByCode(Integer code) {
        if (code == null) {
            return OTHER.getDesc();
        }
        for (TagCategoryEnum category : values()) {
            if (category.getCode().equals(code)) {
                return category.getDesc();
            }
        }
        return OTHER.getDesc();
    }
}
