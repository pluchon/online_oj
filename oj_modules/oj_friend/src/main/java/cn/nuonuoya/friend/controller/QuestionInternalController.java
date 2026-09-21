package cn.nuonuoya.friend.controller;

import cn.nuonuoya.api.friend.api.FriendQuestionInternalApi;
import cn.nuonuoya.friend.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

// C端题目内部接口控制器（网关已屏蔽 internal 路径，仅供服务间调用）
@RestController
public class QuestionInternalController implements FriendQuestionInternalApi {

    @Autowired
    private QuestionService questionService;

    /** 刷新题目顺序缓存与ES索引 */
    @Override
    public Integer refreshQuestionData() {
        return questionService.refreshQuestionData();
    }
}
