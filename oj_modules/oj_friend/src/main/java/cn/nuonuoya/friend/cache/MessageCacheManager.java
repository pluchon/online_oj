package cn.nuonuoya.friend.cache;

import cn.nuonuoya.friend.domain.TbMessage;
import cn.nuonuoya.friend.enums.MessageReadStatusEnum;
import cn.nuonuoya.friend.mapper.MessageMapper;
import cn.nuonuoya.redis.service.RedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

// 站内消息缓存管理器（用户未读计数）
@Slf4j
@Component
public class MessageCacheManager {

    @Autowired
    private RedisService redisService;

    @Autowired
    private MessageMapper messageMapper;

    // 用户未读计数Key前缀 (u:m:unread:{userId})
    public static final String USER_UNREAD_PREFIX = "u:m:unread:";

    // 缓存默认保留天数（7天）
    private static final long CACHE_TTL_DAYS = 7;

    // 获取用户未读数Key
    private String getUserUnreadKey(Long userId) {
        return USER_UNREAD_PREFIX + userId;
    }

    // 获取当前用户未读消息数量
    public int getUnreadCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        String unreadKey = getUserUnreadKey(userId);
        Integer count = redisService.getCacheObject(unreadKey, Integer.class);
        if (count != null) {
            return Math.max(0, count);
        }

        // 缓存未命中，回查数据库实际未读数
        Long dbCount = messageMapper.selectCount(new LambdaQueryWrapper<TbMessage>()
                .eq(TbMessage::getRecId, userId)
                .eq(TbMessage::getIsRead, MessageReadStatusEnum.UNREAD.getCode()));
        int actualCount = dbCount != null ? dbCount.intValue() : 0;
        redisService.setCacheObject(unreadKey, actualCount, CACHE_TTL_DAYS, TimeUnit.DAYS);
        return actualCount;
    }

    // 增加用户未读消息计数
    public void incrementUnreadCount(Long userId) {
        if (userId == null) {
            return;
        }
        String unreadKey = getUserUnreadKey(userId);
        Boolean hasKey = redisService.hasKey(unreadKey);
        if (Boolean.TRUE.equals(hasKey)) {
            redisService.increment(unreadKey);
        } else {
            getUnreadCount(userId);
        }
    }

    // 减少用户未读消息计数
    public void decrementUnreadCount(Long userId) {
        if (userId == null) {
            return;
        }
        String unreadKey = getUserUnreadKey(userId);
        Integer count = redisService.getCacheObject(unreadKey, Integer.class);
        if (count != null && count > 0) {
            redisService.increment(unreadKey, -1);
        } else {
            redisService.setCacheObject(unreadKey, 0, CACHE_TTL_DAYS, TimeUnit.DAYS);
        }
    }

    // 清空用户未读数（全部标为已读时）
    public void clearUnreadCount(Long userId) {
        if (userId != null) {
            redisService.setCacheObject(getUserUnreadKey(userId), 0, CACHE_TTL_DAYS, TimeUnit.DAYS);
        }
    }
}
