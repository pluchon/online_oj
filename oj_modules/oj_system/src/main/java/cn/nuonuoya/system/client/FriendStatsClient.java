package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.vo.FriendOverviewVO;
import cn.nuonuoya.common.enums.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// C端统计服务调用封装（远程不可用或提供方出错时抛出 3503）
@Component
public class FriendStatsClient {

    @Autowired
    private FriendStatsFeignClient friendStatsFeignClient;

    // 数据概览统计
    public FriendOverviewVO getOverview() {
        return RemoteCallGuard.call("查询数据概览", ResultCode.FAILED_STATS_UNAVAILABLE,
                () -> friendStatsFeignClient.getOverview(),
                overview -> overview != null && overview.getDailyTrend() != null);
    }
}
