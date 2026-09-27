package cn.nuonuoya.system.service;

import cn.nuonuoya.system.vo.OverviewVO;

// 数据概览业务接口（统计来自 oj-friend，本服务补题目与竞赛信息）
public interface OverviewService {

    // 查询数据概览
    OverviewVO getOverview();
}
