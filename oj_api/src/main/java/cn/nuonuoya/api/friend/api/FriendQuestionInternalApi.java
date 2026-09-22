package cn.nuonuoya.api.friend.api;

import cn.nuonuoya.api.friend.dto.FriendQuestionCandidateQueryDTO;
import cn.nuonuoya.api.friend.vo.FriendQuestionCandidateVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

// C端题目内部接口契约（调用方：oj-system；提供方：oj-friend。刷新为写操作：题目增删改后刷新缓存与ES索引；候选检索为只读：AI 帮建竞赛选题）
public interface FriendQuestionInternalApi {

    // 刷新题目顺序缓存与ES索引，返回同步题数
    @PostMapping("/friend/internal/question/refresh")
    Integer refreshQuestionData();

    // 按检索词与难度混合检索候选题目（向量 + 关键词，数量不足时同难度补齐），只读
    @PostMapping("/friend/internal/question/candidates")
    List<FriendQuestionCandidateVO> listCandidates(@RequestBody FriendQuestionCandidateQueryDTO queryDTO);
}
