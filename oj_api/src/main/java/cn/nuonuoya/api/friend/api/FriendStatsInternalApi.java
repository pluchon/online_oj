package cn.nuonuoya.api.friend.api;

import cn.nuonuoya.api.friend.vo.FriendDailyStatVO;
import cn.nuonuoya.api.friend.vo.FriendExamSummaryVO;
import cn.nuonuoya.api.friend.vo.FriendFailedSampleVO;
import cn.nuonuoya.api.friend.vo.FriendHardAnalysisVO;
import cn.nuonuoya.api.friend.vo.FriendOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// C端统计内部接口契约（提供方：oj-friend；调用方：oj-system 数据概览与难题分析；只读）
public interface FriendStatsInternalApi {

    // 汇总今日与近 7 天的提交统计与难题榜
    @GetMapping("/friend/internal/stats/overview")
    FriendOverviewVO getOverview();

    // 近 N 天（含今日）每日提交趋势，按日期升序，没有提交的日子补 0
    @GetMapping("/friend/internal/stats/trend")
    List<FriendDailyStatVO> getTrend(@RequestParam("days") Integer days);

    // 指定竞赛的报名与参赛人数：每场各自统计，汇总按用户去重（竞赛范围由调用方按时间段选出）
    @PostMapping("/friend/internal/stats/exam")
    FriendExamSummaryVO getExamSummary(@RequestBody List<Long> examIds);

    // 难题分析统计：已出结论的提交满 5 条的题，给出单题、按标签、按判题结论三组数字
    @GetMapping("/friend/internal/stats/hard-analysis")
    FriendHardAnalysisVO getHardAnalysis();

    // 某题最近的未通过提交样本（每个学员取最近一份；caseId 不为空时只取首个未通过用例是它的；limit 为 1 ~ 5）
    @GetMapping("/friend/internal/stats/failed-samples")
    List<FriendFailedSampleVO> getFailedSamples(@RequestParam("questionId") Long questionId,
                                                @RequestParam(value = "caseId", required = false) Long caseId,
                                                @RequestParam("limit") Integer limit);
}
