package cn.nuonuoya.friend.controller;

import cn.nuonuoya.api.friend.dto.FriendQuestionCandidateQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendQuestionCandidateVO;
import org.springframework.web.bind.annotation.RequestBody;
import cn.nuonuoya.api.friend.api.FriendQuestionInternalApi;
import cn.nuonuoya.friend.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    /** AI 帮建竞赛的候选题目 */
    @Override
    public List<FriendQuestionCandidateVO> listCandidates(@RequestBody FriendQuestionCandidateQueryDTO queryDTO) {
        return questionService.listCandidates(queryDTO);
    }
}
