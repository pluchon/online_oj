package cn.nuonuoya.friend.cache;

import cn.hutool.core.collection.CollUtil;
import cn.nuonuoya.friend.domain.TbMessage;
import cn.nuonuoya.friend.domain.TbMessageText;
import cn.nuonuoya.friend.enums.MessageReadStatusEnum;
import cn.nuonuoya.friend.mapper.MessageMapper;
import cn.nuonuoya.friend.mapper.MessageTextMapper;
import cn.nuonuoya.redis.service.RedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// 站内消息多级缓存管理器（基于用户列表List与正文String双层架构）
@Slf4j
@Component
public class MessageCacheManager {

    @Autowired
    private RedisService redisService;

    @Autowired
    private MessageTextMapper messageTextMapper;

    @Autowired
    private MessageMapper messageMapper;

    // 用户消息ID列表Key前缀 (u:m:l:{userId})
    public static final String USER_MSG_LIST_PREFIX = "u:m:l:";

    // 消息详情Key前缀 (m:d:{textId})
    public static final String MSG_DETAIL_PREFIX = "m:d:";

    // 用户未读计数Key前缀 (u:m:unread:{userId})
    public static final String USER_UNREAD_PREFIX = "u:m:unread:";

    // 缓存默认保留天数（7天）
    private static final long CACHE_TTL_DAYS = 7;

    // 获取用户消息列表Key
    private String getUserMsgListKey(Long userId) {
        return USER_MSG_LIST_PREFIX + userId;
    }

    // 获取消息详情Key
    private String getMsgDetailKey(Long textId) {
        return MSG_DETAIL_PREFIX + textId;
    }

    // 获取用户未读数Key
    private String getUserUnreadKey(Long userId) {
        return USER_UNREAD_PREFIX + userId;
    }

    // 获取消息正文详情（优先Redis String，未命中回查MySQL并回填）
    public TbMessageText getMessageText(Long textId) {
        if (textId == null) {
            return null;
        }
        String key = getMsgDetailKey(textId);
        TbMessageText detail = redisService.getCacheObject(key, TbMessageText.class);
        if (detail != null) {
            return detail;
        }

        detail = messageTextMapper.selectById(textId);
        if (detail != null) {
            redisService.setCacheObject(key, detail, CACHE_TTL_DAYS, TimeUnit.DAYS);
        }
        return detail;
    }

    // 缓存消息正文
    public void saveMessageTextCache(TbMessageText messageText) {
        if (messageText != null && messageText.getTextId() != null) {
            redisService.setCacheObject(getMsgDetailKey(messageText.getTextId()), messageText, CACHE_TTL_DAYS, TimeUnit.DAYS);
        }
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

    // 向用户消息队列左侧推入最新消息ID
    public void pushUserMessage(Long userId, Long textId) {
        if (userId == null || textId == null) {
            return;
        }
        String listKey = getUserMsgListKey(userId);
        redisService.leftPushForList(listKey, textId);
        // 修剪列表，最多保留最新 100 条
        if (redisService.redisTemplate != null) {
            redisService.redisTemplate.opsForList().trim(listKey, 0, 99);
        }
        redisService.expire(listKey, CACHE_TTL_DAYS, TimeUnit.DAYS);
        incrementUnreadCount(userId);
    }

    // 初始化重构用户消息列表缓存
    public void initUserMessageListCache(Long userId) {
        if (userId == null) {
            return;
        }
        String listKey = getUserMsgListKey(userId);
        redisService.deleteObject(listKey);

        // 取最新 100 条投递记录构建初始缓存
        List<TbMessage> list = messageMapper.selectList(new LambdaQueryWrapper<TbMessage>()
                .eq(TbMessage::getRecId, userId)
                .orderByDesc(TbMessage::getMessageId)
                .last("LIMIT 100"));

        if (CollUtil.isNotEmpty(list)) {
            List<Long> textIdList = list.stream().map(TbMessage::getTextId).collect(Collectors.toList());
            redisService.rightPushAll(listKey, textIdList);
            redisService.expire(listKey, CACHE_TTL_DAYS, TimeUnit.DAYS);
        }
    }
}
