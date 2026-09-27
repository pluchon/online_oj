package cn.nuonuoya.system.constants;

// 管理服务的业务缓存键
public final class SystemCacheConstants {

    private SystemCacheConstants() {
    }

    // 难题分析结果（不过期，只在管理员重新分析时覆盖）
    public static final String HARD_ANALYSIS_KEY = "overview:hard_analysis";
}
