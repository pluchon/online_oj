package cn.nuonuoya.api.ai.constants;

// AI 服务中不适合声明为 Feign 契约的内部路径（流式接口由调用方用 WebClient 调用）
public final class AiInternalPaths {

    private AiInternalPaths() {
    }

    // 做题辅导流式对话（text/event-stream；事件 delta 为增量文本，done 为结束与用量，error 为失败）
    public static final String TUTOR_CHAT = "/ai/internal/tutor/chat";

    // 服务名（注册到 Nacos 的应用名）
    public static final String SERVICE_NAME = "oj-ai";

    // 流式事件名：增量文本
    public static final String EVENT_DELTA = "delta";

    // 流式事件名：正常结束（携带模型与用量）
    public static final String EVENT_DONE = "done";

    // 流式事件名：失败
    public static final String EVENT_ERROR = "error";

    // 事件数据字段：增量文本
    public static final String FIELD_TEXT = "text";

    // 事件数据字段：生成回复的模型
    public static final String FIELD_MODEL = "model";

    // 事件数据字段：输入 Token 数
    public static final String FIELD_PROMPT_TOKENS = "promptTokens";

    // 事件数据字段：输出 Token 数
    public static final String FIELD_COMPLETION_TOKENS = "completionTokens";

    // 事件数据字段：错误提示
    public static final String FIELD_MSG = "msg";
}
