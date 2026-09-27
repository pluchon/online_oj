package cn.nuonuoya.friend.service;

import cn.nuonuoya.api.friend.vo.FriendOverviewVO;

// 统计业务接口（供管理端数据概览使用）
public interface StatsService {

    // 汇总今日与近 7 天的提交统计、每日趋势、难题榜与最近一场竞赛的参与情况
    FriendOverviewVO getOverview();
}
