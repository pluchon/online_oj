package cn.nuonuoya.job.enums;

import lombok.Getter;

// 竞赛发布状态枚举
@Getter
public enum ExamPublishStatusEnum {

    // 未发布
    UNPUBLISHED(0, "未发布"),

    // 已发布
    PUBLISHED(1, "已发布");

    // 状态编码
    private final Integer code;

    // 状态描述
    private final String desc;

    ExamPublishStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
