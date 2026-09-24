package cn.nuonuoya.friend.constants;

// Sentinel 资源名（规则在 Nacos 的 oj-friend-sentinel-flow.yaml / oj-friend-sentinel-degrade.yaml 中按资源名配置）
public final class SentinelResources {

    private SentinelResources() {
    }

    // 同步运行判题
    public static final String JUDGE_RUN = "friend-judge-run";

    // AI 辅导流式对话
    public static final String AI_TUTOR = "friend-ai-tutor";

    // AI 向量计算
    public static final String AI_EMBEDDING = "friend-ai-embedding";

    // AI 内容审核
    public static final String AI_MODERATION = "friend-ai-moderation";
}
