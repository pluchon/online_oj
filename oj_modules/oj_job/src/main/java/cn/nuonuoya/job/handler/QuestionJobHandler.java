package cn.nuonuoya.job.handler;

import cn.nuonuoya.job.client.SystemQuestionFeignClient;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 题目定时任务处理器（调用 B 端题目内部接口，失败时标记任务失败由调度中心重试）
@Slf4j
@Component
public class QuestionJobHandler {

    @Autowired
    private SystemQuestionFeignClient systemQuestionFeignClient;

    // 定时公开已结束竞赛的题目：所在竞赛全部结束的竞赛题改为刷题，进入 C 端题库（重复执行不会重复处理）
    @XxlJob("questionPublishHandler")
    public void questionPublishHandler() {
        try {
            Integer published = systemQuestionFeignClient.publishFinishedContestQuestions();
            XxlJobHelper.handleSuccess("本次公开题目 " + published + " 道");
        } catch (Exception e) {
            log.error("公开已结束竞赛的题目失败", e);
            XxlJobHelper.handleFail("公开已结束竞赛的题目失败: " + e.getMessage());
        }
    }
}
