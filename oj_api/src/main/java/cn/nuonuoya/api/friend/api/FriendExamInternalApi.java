package cn.nuonuoya.api.friend.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// C端竞赛内部接口契约（提供方：oj-friend；调用方：oj-system 变更竞赛后、oj-job 定时任务；写操作）
public interface FriendExamInternalApi {

    // 刷新竞赛列表缓存；examId 非空时同时清除该竞赛的详情与题目顺序缓存；返回列表中的竞赛总数
    @PostMapping("/friend/internal/exam/cache/refresh")
    Integer refreshExamCache(@RequestParam(value = "examId", required = false) Long examId);

    // 结算所有已结束且未结算的竞赛（幂等），返回本次结算场数
    @PostMapping("/friend/internal/exam/rank/settle")
    Integer settleFinishedExams();
}
