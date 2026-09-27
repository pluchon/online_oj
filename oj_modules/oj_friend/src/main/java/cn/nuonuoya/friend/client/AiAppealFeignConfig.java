package cn.nuonuoya.friend.client;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

// 申诉 AI 初审 Feign 专用配置（模型推理较慢，读取超时放宽；不加 @Configuration，只作用于 AiAppealFeignClient）
public class AiAppealFeignConfig {

    // 连接与读取超时，可在 Nacos 中通过 oj.ai.appeal.* 覆盖
    @Bean
    public Request.Options aiAppealRequestOptions(@Value("${oj.ai.appeal.connect-timeout-ms:3000}") long connectTimeoutMs,
                                                  @Value("${oj.ai.appeal.read-timeout-ms:60000}") long readTimeoutMs) {
        return new Request.Options(connectTimeoutMs, TimeUnit.MILLISECONDS, readTimeoutMs, TimeUnit.MILLISECONDS, true);
    }
}
