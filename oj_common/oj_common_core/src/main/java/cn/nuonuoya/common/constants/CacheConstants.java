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
}