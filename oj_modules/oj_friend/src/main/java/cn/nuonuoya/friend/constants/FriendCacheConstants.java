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

    // 运行示例用例限流键前缀（q:run:limit:{userId}）
    public final static String QUESTION_RUN_LIMIT_KEY = "q:run:limit:";
}
