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
}