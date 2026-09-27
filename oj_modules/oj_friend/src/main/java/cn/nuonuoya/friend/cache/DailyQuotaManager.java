package cn.nuonuoya.friend.cache;

import cn.nuonuoya.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

// 按自然日的次数计数（AI 辅导、申诉初审、正式申诉共用；先占用再执行，失败时归还）
@Component
public class DailyQuotaManager {

    // 计数键保留天数（跨过零点后自然过期）
    private static final long KEY_TTL_DAYS = 2;

    // 日期格式
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Autowired
    private RedisService redisService;

    // 今日剩余次数
    public int getRemaining(String keyPrefix, int dailyLimit, Long userId) {
        Integer used = redisService.getCacheObject(key(keyPrefix, userId), Integer.class);
        return Math.max(0, dailyLimit - (used == null ? 0 : used));
    }

    // 占用一次，超出上限时归还并返回 false
    public boolean tryAcquire(String keyPrefix, int dailyLimit, Long userId) {
        String key = key(keyPrefix, userId);
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

    // 归还一次（后续步骤失败时）
    public void release(String keyPrefix, Long userId) {
        redisService.increment(key(keyPrefix, userId), -1);
    }

    // 当日计数键（{前缀}{userId}:{yyyyMMdd}）
    private String key(String keyPrefix, Long userId) {
        return keyPrefix + userId + ":" + LocalDate.now().format(DAY_FORMATTER);
    }
}
