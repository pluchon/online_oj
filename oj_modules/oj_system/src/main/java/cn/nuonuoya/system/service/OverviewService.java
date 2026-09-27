package cn.nuonuoya.system.service;

import cn.nuonuoya.system.dto.OverviewExamQueryDTO;
import cn.nuonuoya.system.enums.OverviewTrendRange;
import cn.nuonuoya.system.vo.OverviewExamSummaryVO;
import cn.nuonuoya.system.vo.OverviewTrendVO;
import cn.nuonuoya.system.vo.OverviewVO;

import java.util.List;

// 数据概览业务接口（提交与报名统计来自 oj-friend，本服务补题目与竞赛信息）
public interface OverviewService {

    // 查询数据概览（今日、近 7 天、难题榜）
    OverviewVO getOverview();

    // 按时间范围分段的提交趋势
    List<OverviewTrendVO> getTrend(OverviewTrendRange range);

    // 近 N 天内进行过的竞赛：汇总人数与分页列表
    OverviewExamSummaryVO getExamSummary(OverviewExamQueryDTO queryDTO);
}
