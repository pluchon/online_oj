package cn.nuonuoya.api.friend.api;

import cn.nuonuoya.api.friend.vo.FriendOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;

// C端统计内部接口契约（提供方：oj-friend；调用方：oj-system 数据概览；只读）
public interface FriendStatsInternalApi {

    // 汇总今日与近 7 天的提交统计、每日趋势、难题榜与最近一场竞赛的参与情况
    @GetMapping("/friend/internal/stats/overview")
    FriendOverviewVO getOverview();
}
