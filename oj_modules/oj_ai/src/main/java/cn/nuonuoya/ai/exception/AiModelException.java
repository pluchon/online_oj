package cn.nuonuoya.ai.exception;

// 模型调用失败（网络、超时、限流或返回内容无法解析），对外表现为 503
public class AiModelException extends RuntimeException {

    // 携带失败原因与底层异常
    public AiModelException(String message, Throwable cause) {
        super(message, cause);
    }

    // 仅携带失败原因
    public AiModelException(String message) {
        super(message);
    }
}
