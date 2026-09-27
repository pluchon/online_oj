package cn.nuonuoya.friend.controller;

import cn.nuonuoya.api.friend.api.FriendStatsInternalApi;
import cn.nuonuoya.api.friend.vo.FriendDailyStatVO;
import cn.nuonuoya.api.friend.vo.FriendExamSummaryVO;
import cn.nuonuoya.api.friend.vo.FriendFailedSampleVO;
import cn.nuonuoya.api.friend.vo.FriendHardAnalysisVO;
import cn.nuonuoya.api.friend.vo.FriendOverviewVO;
import cn.nuonuoya.friend.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// C端统计内部接口控制器（网关已屏蔽 internal 路径，仅供服务间调用）
@RestController
public class StatsInternalController implements FriendStatsInternalApi {

    @Autowired
    private StatsService statsService;

    /** 数据概览统计 */
    @Override
    public FriendOverviewVO getOverview() {
        return statsService.getOverview();
    }

    /** 近 N 天每日提交趋势 */
    @Override
    public List<FriendDailyStatVO> getTrend(@RequestParam("days") Integer days) {
        return statsService.getTrend(days);
    }

    /** 指定竞赛的报名与参赛人数 */
    @Override
    public FriendExamSummaryVO getExamSummary(@RequestBody List<Long> examIds) {
        return statsService.getExamSummary(examIds);
    }

    /** 难题分析统计 */
    @Override
    public FriendHardAnalysisVO getHardAnalysis() {
        return statsService.getHardAnalysis();
    }

    /** 某题最近的未通过提交样本 */
    @Override
    public List<FriendFailedSampleVO> getFailedSamples(@RequestParam("questionId") Long questionId,
                                                       @RequestParam(value = "caseId", required = false) Long caseId,
                                                       @RequestParam("limit") Integer limit) {
        return statsService.getFailedSamples(questionId, caseId, limit);
    }
}
