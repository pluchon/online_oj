package cn.nuonuoya.api.system.api;

import org.springframework.web.bind.annotation.PostMapping;

// B端题目内部接口契约（提供方：oj-system；调用方：oj-job 定时任务；写操作）
public interface SystemQuestionInternalApi {

    // 公开已结束竞赛的题目：所在竞赛全部结束的竞赛题改为刷题（幂等），返回本次公开的题数
    @PostMapping("/system/internal/question/publish")
    Integer publishFinishedContestQuestions();
}
