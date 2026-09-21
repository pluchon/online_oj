package cn.nuonuoya.system.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// C端用户服务调用封装（失败时返回 false，由调用方决定后续处理）
@Slf4j
@Component
public class FriendUserClient {

    @Autowired
    private FriendUserFeignClient friendUserFeignClient;

    // 清除C端用户详情缓存，成功返回 true
    public boolean evictUserCache(Long userId) {
        try {
            return Boolean.TRUE.equals(friendUserFeignClient.evictUserCache(userId));
        } catch (Exception e) {
            log.error("清除C端用户缓存失败, userId = {}, error = {}", userId, e.getMessage());
            return false;
        }
    }
}
