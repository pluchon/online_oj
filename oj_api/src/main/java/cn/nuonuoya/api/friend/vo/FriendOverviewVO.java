package cn.nuonuoya.api.friend.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// 数据概览统计结果
@Getter
@Setter
@ToString
public class FriendOverviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 今日统计
    private FriendPeriodStatVO today;

    // 近 7 天统计（含今日）
    private FriendPeriodStatVO week;

    // 近 7 天每日趋势（按日期升序，没有提交的日子补 0）
    private List<FriendDailyStatVO> dailyTrend;

    // 难题榜：已出结论提交数达到门槛的题中通过率最低的若干道
    private List<FriendQuestionStatVO> hardQuestions;

    // 最近一场已开赛的竞赛（没有时为空）
    private FriendExamStatVO latestExam;
}
