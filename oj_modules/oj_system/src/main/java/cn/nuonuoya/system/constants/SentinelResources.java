package cn.nuonuoya.system.constants;

// Sentinel 资源名（规则在 Nacos 的 oj-system-sentinel-flow.yaml / oj-system-sentinel-degrade.yaml 中按资源名配置）
public final class SentinelResources {

    private SentinelResources() {
    }

    // AI 出题相关调用（题面、用例输入、解法、竞赛帮建）
    public static final String AI = "system-ai";

    // 运行标程得到用例输出
    public static final String JUDGE_RUN = "system-judge-run";
}
