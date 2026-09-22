package cn.nuonuoya.ai.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 按模型构造 DashScope 调用参数：多模态模型自动切换到多模态接口
@Component
public class AiChatOptionsFactory {

    @Autowired
    private AiProperties aiProperties;

    // 指定模型与温度的参数构建器，调用方可继续追加其他参数
    public DashScopeChatOptions.DashScopeChatOptionsBuilder builder(String model, Double temperature) {
        return DashScopeChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .multiModel(aiProperties.getMultimodalModels().contains(model))
                .enableThinking(aiProperties.getEnableThinking());
    }
}
