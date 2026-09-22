package cn.nuonuoya.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// AI 客户端装配
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    // 出题类功能使用的对话客户端（模型与温度在每次调用时按配置指定）
    @Bean
    public ChatClient questionChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    // 做题辅导使用的对话客户端
    @Bean
    public ChatClient tutorChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    // 内容审核使用的对话客户端
    @Bean
    public ChatClient moderationChatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
