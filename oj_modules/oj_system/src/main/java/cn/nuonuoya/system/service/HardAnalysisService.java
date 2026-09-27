package cn.nuonuoya.system.service;

import cn.nuonuoya.system.vo.OverviewHardAnalysisVO;

// 难题分析业务接口（统计来自 oj-friend，结论来自 oj-ai，结果缓存在 Redis）
public interface HardAnalysisService {

    // 读取上一次的分析结果，没有时返回 null
    OverviewHardAnalysisVO getLatest();

    // 重新统计并分析，数据足够时覆盖缓存
    OverviewHardAnalysisVO analyze();
}
