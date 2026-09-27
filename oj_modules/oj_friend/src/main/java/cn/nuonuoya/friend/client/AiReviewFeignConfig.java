package cn.nuonuoya.friend.client;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

// 赛后复盘 Feign 专用配置（要点评整场所有题，读取超时放宽；不加 @Configuration，只作用于 AiReviewFeignClient）
public class AiReviewFeignConfig {

    // 连接与读取超时，可在 Nacos 中通过 oj.ai.review.* 覆盖
    @Bean
    public Request.Options aiReviewRequestOptions(@Value("${oj.ai.review.connect-timeout-ms:3000}") long connectTimeoutMs,
                                                  @Value("${oj.ai.review.read-timeout-ms:120000}") long readTimeoutMs) {
        return new Request.Options(connectTimeoutMs, TimeUnit.MILLISECONDS, readTimeoutMs, TimeUnit.MILLISECONDS, true);
    }
}
