package cn.nuonuoya.friend.client;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

// AI 审核接口 Feign 专用配置（审核同步阻塞用户操作，超时较短，超时即放行）
public class AiModerationFeignConfig {

    // 连接与读取超时，可在 Nacos 中通过 oj.ai.moderation.* 覆盖
    @Bean
    public Request.Options aiModerationRequestOptions(@Value("${oj.ai.moderation.connect-timeout-ms:2000}") long connectTimeoutMs,
                                                      @Value("${oj.ai.moderation.read-timeout-ms:8000}") long readTimeoutMs) {
        return new Request.Options(connectTimeoutMs, TimeUnit.MILLISECONDS, readTimeoutMs, TimeUnit.MILLISECONDS, true);
    }
}
