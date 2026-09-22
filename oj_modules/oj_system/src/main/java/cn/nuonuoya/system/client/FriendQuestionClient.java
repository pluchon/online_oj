package cn.nuonuoya.system.client;

import cn.nuonuoya.api.friend.dto.FriendQuestionCandidateQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendQuestionCandidateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

// C端题目服务调用封装（失败时返回 false，由调用方决定后续处理）
@Slf4j
@Component
public class FriendQuestionClient {

    @Autowired
    private FriendQuestionFeignClient friendQuestionFeignClient;

    // 检索 AI 帮建竞赛的候选题目，失败返回空列表
    public List<FriendQuestionCandidateVO> listCandidates(FriendQuestionCandidateQueryDTO queryDTO) {
        try {
            List<FriendQuestionCandidateVO> list = friendQuestionFeignClient.listCandidates(queryDTO);
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            log.error("检索候选题目失败, error = {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // 通知C端刷新题目缓存与ES索引，成功返回 true
    public boolean refreshQuestionData() {
        try {
            friendQuestionFeignClient.refreshQuestionData();
            return true;
        } catch (Exception e) {
            log.error("通知C端刷新题目数据失败, error = {}", e.getMessage());
            return false;
        }
    }
}
