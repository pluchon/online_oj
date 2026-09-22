package cn.nuonuoya.friend.cache;

import cn.nuonuoya.friend.constants.FriendCacheConstants;
import cn.nuonuoya.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

// AI 辅导每日次数计数（按自然日，先占用再调用，失败时归还）
@Component
public class AiTutorQuotaManager {

    // 计数键保留天数（跨过零点后自然过期）
    private static final long KEY_TTL_DAYS = 2;

    // 日期格式
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Autowired
    private RedisService redisService;

    // 每人每日可提问次数，可在 Nacos 中覆盖
    @Value("${oj.ai.tutor.daily-limit:30}")
    private int dailyLimit;

    // 每日可提问次数
    public int getDailyLimit() {
        return dailyLimit;
    }

    // 今日剩余次数
    public int getRemaining(Long userId) {
        Integer used = redisService.getCacheObject(key(userId), Integer.class);
        return Math.max(0, dailyLimit - (used == null ? 0 : used));
    }

    // 占用一次，超出上限时归还并返回 false
    public boolean tryAcquire(Long userId) {
        String key = key(userId);
        Long used = redisService.increment(key);
        if (used != null && used == 1L) {
            redisService.expire(key, KEY_TTL_DAYS, TimeUnit.DAYS);
        }
        if (used == null || used > dailyLimit) {
            redisService.increment(key, -1);
            return false;
        }
        return true;
    }

    // 归还一次（模型调用失败时）
    public void release(Long userId) {
        redisService.increment(key(userId), -1);
    }

    // 当日计数键
    private String key(Long userId) {
        return FriendCacheConstants.AI_TUTOR_QUOTA_KEY + userId + ":" + LocalDate.now().format(DAY_FORMATTER);
    }
}
