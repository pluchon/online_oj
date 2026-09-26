package cn.nuonuoya.system.controller;

import cn.nuonuoya.api.system.api.SystemQuestionInternalApi;
import cn.nuonuoya.system.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

// B端题目内部接口控制器（网关屏蔽 internal 路径，只供服务间调用）
@RestController
public class QuestionInternalController implements SystemQuestionInternalApi {

    @Autowired
    private QuestionService questionService;

    /** 公开已结束竞赛的题目 */
    @Override
    public Integer publishFinishedContestQuestions() {
        return questionService.publishFinishedContestQuestions();
    }
}
