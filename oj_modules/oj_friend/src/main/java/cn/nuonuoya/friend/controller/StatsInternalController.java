package cn.nuonuoya.friend.controller;

import cn.nuonuoya.api.friend.api.FriendStatsInternalApi;
import cn.nuonuoya.api.friend.vo.FriendOverviewVO;
import cn.nuonuoya.friend.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

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
}
