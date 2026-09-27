package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.vo.FriendDailyStatVO;
import cn.nuonuoya.api.friend.vo.FriendExamSummaryVO;
import cn.nuonuoya.api.friend.vo.FriendOverviewVO;
import cn.nuonuoya.common.enums.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

// C端统计服务调用封装（远程不可用或提供方出错时抛出 3503）
@Component
public class FriendStatsClient {

    @Autowired
    private FriendStatsFeignClient friendStatsFeignClient;

    // 数据概览统计
    public FriendOverviewVO getOverview() {
        return RemoteCallGuard.call("查询数据概览", ResultCode.FAILED_STATS_UNAVAILABLE,
                () -> friendStatsFeignClient.getOverview(),
                overview -> overview != null && overview.getToday() != null && overview.getWeek() != null);
    }

    // 近 N 天每日提交趋势（提供方按天补齐，条数不等于天数视为出错）
    public List<FriendDailyStatVO> getTrend(int days) {
        return RemoteCallGuard.call("查询提交趋势", ResultCode.FAILED_STATS_UNAVAILABLE,
                () -> friendStatsFeignClient.getTrend(days),
                trend -> trend != null && trend.size() == days);
    }

    // 指定竞赛的报名与参赛人数
    public FriendExamSummaryVO getExamSummary(List<Long> examIds) {
        return RemoteCallGuard.call("查询竞赛人数", ResultCode.FAILED_STATS_UNAVAILABLE,
                () -> friendStatsFeignClient.getExamSummary(examIds),
                summary -> summary != null && summary.getEnrollCount() != null && summary.getExams() != null);
    }
}
