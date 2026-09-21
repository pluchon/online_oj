package cn.nuonuoya.friend.enums;

import lombok.Getter;

import java.time.LocalDateTime;

// 竞赛进行状态枚举（按当前时间与起止时间动态计算）
@Getter
public enum ExamContestStatusEnum {

    // 未开赛
    NOT_STARTED(0, "未开赛", "报名参赛"),

    // 进行中
    ONGOING(1, "进行中", "进入竞赛"),

    // 已完赛
    FINISHED(2, "已完赛", "已完赛");

    // 状态编码
    private final Integer code;

    // 状态描述
    private final String desc;

    // 竞赛列表操作按钮文案
    private final String btnText;

    ExamContestStatusEnum(Integer code, String desc, String btnText) {
        this.code = code;
        this.desc = desc;
        this.btnText = btnText;
    }

    // 根据起止时间与当前时间计算竞赛状态
    public static ExamContestStatusEnum of(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime now) {
        if (startTime != null && now.isBefore(startTime)) {
            return NOT_STARTED;
        }
        if (endTime != null && !now.isAfter(endTime)) {
            return ONGOING;
        }
        return FINISHED;
    }
}
