package cn.nuonuoya.friend.client;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

// AI 向量接口 Feign 专用配置（不加 @Configuration，只作用于 AiSearchFeignClient）
public class AiSearchFeignConfig {

    // 连接与读取超时，可在 Nacos 中通过 oj.ai.search.* 覆盖
    @Bean
    public Request.Options aiSearchRequestOptions(@Value("${oj.ai.search.connect-timeout-ms:3000}") long connectTimeoutMs,
                                                  @Value("${oj.ai.search.read-timeout-ms:20000}") long readTimeoutMs) {
        return new Request.Options(connectTimeoutMs, TimeUnit.MILLISECONDS, readTimeoutMs, TimeUnit.MILLISECONDS, true);
    }
}
