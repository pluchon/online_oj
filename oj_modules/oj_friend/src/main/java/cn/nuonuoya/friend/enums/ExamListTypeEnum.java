package cn.nuonuoya.friend.enums;

import lombok.Getter;

// 竞赛列表分类枚举
@Getter
public enum ExamListTypeEnum {

    // 未完赛（未开赛与进行中）
    UNFINISHED(0, "未完赛"),

    // 历史竞赛（已完赛）
    HISTORY(1, "历史竞赛");

    // 分类编码
    private final Integer code;

    // 分类描述
    private final String desc;

    ExamListTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
