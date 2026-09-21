package cn.nuonuoya.common.constants;

// 跨服务共用的缓存常量（登录会话，业务缓存键放在各服务本地）
public class CacheConstants {

    // 登录令牌缓存有效期（分钟）
    public final static long EXPIRATION = 720;

    // 登录令牌缓存键前缀（login_tokens:{userKey}）
    public final static String LOGIN_TOKEN_KEY = "login_tokens:";

    // 令牌剩余有效期低于该值（分钟）时自动续期
    public final static long TOKEN_REFRESH_TIME = 180;
}
