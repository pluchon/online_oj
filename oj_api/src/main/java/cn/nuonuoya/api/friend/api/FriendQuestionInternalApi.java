package cn.nuonuoya.api.friend.api;

import org.springframework.web.bind.annotation.PostMapping;

// C端题目内部接口契约（调用方：oj-system 题目增删改后；提供方：oj-friend；写操作，刷新题目缓存与ES索引）
public interface FriendQuestionInternalApi {

    // 刷新题目顺序缓存与ES索引，返回同步题数
    @PostMapping("/friend/internal/question/refresh")
    Integer refreshQuestionData();
}
