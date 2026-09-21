package cn.nuonuoya.common.constants;

// 缓存相关常量
public class CacheConstants {

    /**
     * 缓存有效期，默认720（分钟）
     */
    public final static long EXPIRATION = 720;

    /**
     * ⽤⼾⾝份认证缓存前缀
     */
    public final static String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 令牌剩余时间的刷新临界值，三小时
     */
    public final static long TOKEN_REFRESH_TIME = 180;

    /**
     * 短信验证码缓存前缀（sms_code:{phone}）
     */
    public final static String SMS_CODE_KEY = "sms_code:";

    /**
     * 短信验证码发送冷却时间缓存前缀（sms_code_interval:{phone}）
     */
    public final static String SMS_CODE_INTERVAL_KEY = "sms_code_interval:";

    /**
     * 手机号单日发送次数计数缓存前缀（sms_code_count:{phone}）
     */
    public final static String SMS_CODE_COUNT_KEY = "sms_code_count:";

    /**
     * 未完赛竞赛ID列表缓存键（exam:unfinish:list）
     */
    public final static String EXAM_UNFINISH_LIST_KEY = "exam:unfinish:list";

    /**
     * 历史竞赛ID列表缓存键（exam:history:list）
     */
    public final static String EXAM_HISTORY_LIST_KEY = "exam:history:list";

    /**
     * 竞赛详情缓存键前缀（exam:detail:{examId}）
     */
    public final static String EXAM_DETAIL_KEY = "exam:detail:";

    // 用户已报名竞赛ID列表缓存键前缀（user:exam:list:{userId}）
    public final static String USER_EXAM_LIST_KEY = "user:exam:list:";

    // 题目顺序列表缓存键（q:l）
    public final static String QUESTION_LIST_KEY = "q:l";

    // 运行示例用例限流键前缀（q:run:limit:{userId}）
    public final static String QUESTION_RUN_LIMIT_KEY = "q:run:limit:";

    // 竞赛题目顺序列表缓存键前缀（exam:q:l:{examId}）
    public final static String EXAM_QUESTION_LIST_KEY = "exam:q:l:";

    // 竞赛排名列表缓存键前缀（exam:rank:{examId}）
    public final static String EXAM_RANK_LIST_KEY = "exam:rank:";
}