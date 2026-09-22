package cn.nuonuoya.friend.constants;

// 用户端服务本地缓存常量
public class FriendCacheConstants {

    // 短信验证码缓存键前缀（sms_code:{phone}）
    public final static String SMS_CODE_KEY = "sms_code:";

    // 短信验证码发送冷却缓存键前缀（sms_code_interval:{phone}）
    public final static String SMS_CODE_INTERVAL_KEY = "sms_code_interval:";

    // 手机号单日发送次数缓存键前缀（sms_code_count:{phone}）
    public final static String SMS_CODE_COUNT_KEY = "sms_code_count:";

    // 用户已报名竞赛ID列表缓存键前缀（user:exam:list:{userId}）
    public final static String USER_EXAM_LIST_KEY = "user:exam:list:";

    // 题目顺序列表缓存键（q:l）
    public final static String QUESTION_LIST_KEY = "q:l";

    // 竞赛题目顺序列表缓存键前缀（exam:q:l:{examId}）
    public final static String EXAM_QUESTION_LIST_KEY = "exam:q:l:";

    // AI 辅导每日次数计数键前缀（ai:tutor:quota:{userId}:{yyyyMMdd}）
    public final static String AI_TUTOR_QUOTA_KEY = "ai:tutor:quota:";

    // 运行示例用例限流键前缀（q:run:limit:{userId}）
    public final static String QUESTION_RUN_LIMIT_KEY = "q:run:limit:";

    // 未完赛竞赛ID列表缓存键
    public final static String EXAM_UNFINISH_LIST_KEY = "exam:unfinish:list";

    // 历史竞赛ID列表缓存键
    public final static String EXAM_HISTORY_LIST_KEY = "exam:history:list";

    // 竞赛详情缓存键前缀（exam:detail:{examId}）
    public final static String EXAM_DETAIL_KEY = "exam:detail:";

    // 竞赛排名列表缓存键前缀（exam:rank:{examId}）
    public final static String EXAM_RANK_LIST_KEY = "exam:rank:";

    // 已结束竞赛的排名缓存有效期（小时）
    public final static long EXAM_RANK_FINISHED_TTL_HOURS = 24;

    // 进行中竞赛的排名缓存有效期（分钟）
    public final static long EXAM_RANK_ONGOING_TTL_MINUTES = 3;
}
