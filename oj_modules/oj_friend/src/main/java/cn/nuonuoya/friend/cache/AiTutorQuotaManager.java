package cn.nuonuoya.friend.cache;

import cn.nuonuoya.friend.constants.FriendCacheConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// AI 辅导每日次数计数（计数逻辑见 DailyQuotaManager）
@Component
public class AiTutorQuotaManager {

    @Autowired
    private DailyQuotaManager dailyQuotaManager;

    // 每人每日可提问次数，可在 Nacos 中覆盖
    @Value("${oj.ai.tutor.daily-limit:50}")
    private int dailyLimit;

    // 每日可提问次数
    public int getDailyLimit() {
        return dailyLimit;
    }

    // 今日剩余次数
    public int getRemaining(Long userId) {
        return dailyQuotaManager.getRemaining(FriendCacheConstants.AI_TUTOR_QUOTA_KEY, dailyLimit, userId);
    }

    // 占用一次，超出上限时返回 false
    public boolean tryAcquire(Long userId) {
        return dailyQuotaManager.tryAcquire(FriendCacheConstants.AI_TUTOR_QUOTA_KEY, dailyLimit, userId);
    }

    // 归还一次（模型调用失败时）
    public void release(Long userId) {
        dailyQuotaManager.release(FriendCacheConstants.AI_TUTOR_QUOTA_KEY, userId);
    }
}
