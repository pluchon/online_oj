package cn.nuonuoya.friend.service;

import cn.nuonuoya.api.friend.vo.FriendDailyStatVO;
import cn.nuonuoya.api.friend.vo.FriendExamSummaryVO;
import cn.nuonuoya.api.friend.vo.FriendFailedSampleVO;
import cn.nuonuoya.api.friend.vo.FriendHardAnalysisVO;
import cn.nuonuoya.api.friend.vo.FriendOverviewVO;

import java.util.List;

// 统计业务接口（供管理端数据概览使用）
public interface StatsService {

    // 汇总今日与近 7 天的提交统计与难题榜
    FriendOverviewVO getOverview();

    // 近 N 天每日提交趋势（天数 1 ~ 30）
    List<FriendDailyStatVO> getTrend(Integer days);

    // 指定竞赛的报名与参赛人数
    FriendExamSummaryVO getExamSummary(List<Long> examIds);

    // 难题分析统计：单题、按标签、按判题结论
    FriendHardAnalysisVO getHardAnalysis();

    // 某题最近的未通过提交样本
    List<FriendFailedSampleVO> getFailedSamples(Long questionId, Long caseId, Integer limit);
}
