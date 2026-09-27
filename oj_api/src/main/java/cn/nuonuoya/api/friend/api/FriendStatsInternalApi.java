package cn.nuonuoya.api.friend.api;

import cn.nuonuoya.api.friend.vo.FriendDailyStatVO;
import cn.nuonuoya.api.friend.vo.FriendExamSummaryVO;
import cn.nuonuoya.api.friend.vo.FriendOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// C端统计内部接口契约（提供方：oj-friend；调用方：oj-system 数据概览；只读）
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
}
