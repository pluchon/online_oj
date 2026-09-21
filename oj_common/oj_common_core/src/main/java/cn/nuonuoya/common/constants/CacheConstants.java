package cn.nuonuoya.common.constants;

// 跨服务共用的缓存常量（仅单个服务使用的键放在各服务本地）
public class CacheConstants {

    // 登录令牌缓存有效期（分钟）
    public final static long EXPIRATION = 720;

    // 登录令牌缓存键前缀（login_tokens:{userKey}）
    public final static String LOGIN_TOKEN_KEY = "login_tokens:";

    // 令牌剩余有效期低于该值（分钟）时自动续期
    public final static long TOKEN_REFRESH_TIME = 180;

    // 未完赛竞赛ID列表缓存键（system、job 写入，friend 读取）
    public final static String EXAM_UNFINISH_LIST_KEY = "exam:unfinish:list";

    // 历史竞赛ID列表缓存键（system、job 写入，friend 读取）
    public final static String EXAM_HISTORY_LIST_KEY = "exam:history:list";

    // 竞赛详情缓存键前缀（exam:detail:{examId}）
    public final static String EXAM_DETAIL_KEY = "exam:detail:";
}
