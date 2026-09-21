package cn.nuonuoya.api.friend.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

// C端用户内部接口契约（调用方：oj-system 修改用户状态后；提供方：oj-friend；写操作，仅清除用户详情缓存）
public interface FriendUserInternalApi {

    // 清除指定用户的详情缓存，成功返回 true
    @PostMapping("/friend/internal/user/{userId}/cache/evict")
    Boolean evictUserCache(@PathVariable("userId") Long userId);
}
